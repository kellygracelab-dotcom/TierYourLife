"""Read-only GitHub API: exact CI attempts and data artifact provenance."""
import hashlib
import json
from pathlib import Path
import re
import subprocess

from .artifacts import extract_data
from .policy import (CD_PATH, CI_PATH, OWNER_ID, REPO, REPO_ID, artifact_name,
                     read_json, require)


class GitHub:
    def raw(self, path):
        process = subprocess.run(["gh", "api", f"repos/{REPO}/{path}"], capture_output=True)
        require(process.returncode == 0, "GitHub API read failed")
        return process.stdout

    def get(self, path):
        return json.loads(self.raw(path))

    def pages(self, path, field):
        values = []
        for page in range(1, 101):
            data = self.get(f"{path}{'&' if '?' in path else '?'}per_page=100&page={page}")[field]
            values.extend(data)
            if len(data) < 100:
                return values
        require(False, "GitHub listing exceeds safety bound")

    def fresh(self, source):
        require(self.get("git/ref/heads/main")["object"]["sha"] == source["sha"], "Source is stale; no release mutation was started")
        run = self.get(f"actions/runs/{source['runId']}")
        require(run.get("run_attempt") == source["attempt"] and run.get("conclusion") == "success" and run.get("status") == "completed", "Source CI was rerun or is no longer successful")

    def trusted_run(self, run, source, cd=False):
        require(run.get("path") == (CD_PATH if cd else CI_PATH), "Unexpected workflow path")
        require(run.get("event") == ("workflow_run" if cd else "push"), "Unexpected workflow trigger")
        require(run.get("head_branch") == "main" and run.get("head_sha") == source["sha"], "Unexpected workflow source")
        for field in ("repository", "head_repository"):
            repo = run.get(field, {})
            require(repo.get("id") == REPO_ID and repo.get("owner", {}).get("id") == OWNER_ID, "Untrusted workflow repository")

    def verify_ci(self, source):
        self.fresh(source)
        run = self.get(f"actions/runs/{source['runId']}")
        self.trusted_run(run, source)
        require(run.get("name") == "CI" and run.get("conclusion") == "success" and run.get("status") == "completed", "CI did not complete successfully")
        require(run.get("run_attempt") == source["attempt"], "CI attempt was superseded")
        jobs = self.pages(f"actions/runs/{source['runId']}/attempts/{source['attempt']}/jobs", "jobs")
        return jobs

    def verify_historical_ci(self, source, directory):
        # A rerun of the same source must reuse its prior sealed bytes. Validate
        # the ORIGINAL completed attempt rather than pretending it is latest.
        run = self.get(f"actions/runs/{source['runId']}/attempts/{source['attempt']}")
        self.trusted_run(run, source)
        require(run.get("run_attempt") == source["attempt"] and run.get("status") == "completed" and run.get("conclusion") == "success", "Sealed artifact did not come from successful historical CI")
        jobs = self.pages(f"actions/runs/{source['runId']}/attempts/{source['attempt']}/jobs", "jobs")
        self.ci_artifact("gates", source, directory / "gates", {"gates.json"})
        verify_gates(jobs, read_json(directory / "gates/gates.json"), source)
        self.ci_artifact("unsigned", source, directory / "unsigned", {"bundle.aab", "provenance.json"})

    def download(self, artifact, directory, allowed):
        require(not artifact.get("expired"), "Release artifact expired; build a new main revision")
        require(0 < artifact.get("size_in_bytes", 0) <= 150_000_000, "Invalid artifact size")
        digest = artifact.get("digest", "")
        require(digest.startswith("sha256:") and len(digest) == 71, "Artifact archive digest unavailable")
        data = self.raw(f"actions/artifacts/{int(artifact['id'])}/zip")
        require(len(data) <= 150_000_000 and hashlib.sha256(data).hexdigest() == digest[7:], "Artifact archive digest mismatch")
        archive = Path(directory).parent / f"download-{artifact['id']}.zip"
        archive.parent.mkdir(parents=True, exist_ok=True)
        try:
            archive.write_bytes(data)
            extract_data(archive, directory, allowed)
        finally:
            archive.unlink(missing_ok=True)

    def ci_artifact(self, kind, source, directory, allowed):
        name = artifact_name(kind, source)
        artifacts = self.pages(f"actions/runs/{source['runId']}/artifacts?name={name}", "artifacts")
        matches = [artifact for artifact in artifacts if artifact["name"] == name]
        require(len(matches) == 1, "Missing or ambiguous CI attempt artifact; rerun all CI jobs if a partial rerun omitted the build")
        artifact = matches[0]
        require(artifact.get("workflow_run", {}).get("id") == source["runId"], "Wrong artifact run")
        self.download(artifact, directory, allowed)

    def restore_cd_artifact(self, kind, source, directory, allowed):
        name = artifact_name(kind, source)
        # Intent generations have a run/attempt suffix. List then filter exactly;
        # the REST name parameter is an exact match, not a prefix search.
        artifacts = self.pages("actions/artifacts" if kind == "intent" else f"actions/artifacts?name={name}", "artifacts")
        matches = []
        for artifact in artifacts:
            if not (re.fullmatch(re.escape(name) + r"-[0-9]+-[0-9]+", artifact["name"]) if kind == "intent" else artifact["name"] == name):
                continue
            run_id = artifact.get("workflow_run", {}).get("id")
            require(run_id is not None, "Missing artifact creator")
            run = self.get(f"actions/runs/{int(run_id)}")
            # A PR can forge an artifact NAME; never accept it as a CD receipt.
            if run.get("path") != CD_PATH or run.get("event") != "workflow_run":
                continue
            self.trusted_run(run, source, cd=True)
            matches.append(artifact)
        if kind == "intent":
            # Serialized publisher generations; latest persisted intent wins.
            matches.sort(key=lambda artifact: int(artifact["id"]), reverse=True)
            matches = matches[:1]
        require(len(matches) <= 1, "Conflicting durable CD artifacts; manual investigation required")
        if matches:
            self.download(matches[0], directory, allowed)
        return bool(matches)


def verify_gates(jobs, receipt, source):
    require(receipt.get("source") == source and receipt.get("schema") == 1, "Wrong gate receipt")
    require(receipt.get("build") == receipt.get("changes") == "success", "Build/change gate failed")
    code = receipt.get("code")
    require(code in ("true", "false"), "Unknown applicability of CI gates")
    for key in ("e2e", "instrumentation"):
        require(receipt.get(key) in (("success",) if code == "true" else ("success", "skipped")), "Applicable emulator gate did not pass")
    names = {}
    for job in jobs:
        require(job["name"] not in names, "Ambiguous CI job name")
        names[job["name"]] = job["conclusion"]
    for name in ("Build, unit tests, lint", "What changed", "Android CI passed"):
        require(names.get(name) == "success", f"Required gate failed: {name}")
    if code == "true":
        for name in ("End-to-end against the proxy", "Instrumentation tests (26)", "Instrumentation tests (34)"):
            require(names.get(name) == "success", f"Required gate failed: {name}")
    require(all(result in ("success", "skipped") for result in names.values()), "CI contains an unsuccessful job")


def prepare(github, source, directory):
    directory = Path(directory)
    jobs = github.verify_ci(source)
    github.ci_artifact("gates", source, directory / "gates", {"gates.json"})
    verify_gates(jobs, read_json(directory / "gates/gates.json"), source)
    github.ci_artifact("unsigned", source, directory / "unsigned", {"bundle.aab", "provenance.json"})
    github.fresh(source)

"""Workflow entry points. Secrets are read only by sign / Play API commands."""
import argparse
import os
from pathlib import Path

from . import artifacts
from .github import GitHub, prepare
from .play import PlayApi, Publisher
from .policy import (Stop, artifact_name, output, read_json, require, source_env,
                     sha256, write_json)


def validate_sealed(github, work, source, root):
    original = read_json(work / "signed/provenance.json")["source"]
    require(original.get("sha") == source["sha"], "Sealed artifact is from another main SHA")
    unsigned = work / "original/unsigned"
    if original != source:
        directory = work / "original"
        github.verify_historical_ci(original, directory)
    artifacts.validate_bundle(unsigned, original, root)
    return artifacts.validate_bundle(work / "signed", original, root, signed=True, unsigned=unsigned / "bundle.aab")


def cache_snapshot(work, kind):
    files = [work / "sealed/bundle.enc", work / "sealed/provenance.json"] if kind == "signed" else [work / "intent-sealed/intent.enc"]
    require(all(path.is_file() for path in files), "Checkpoint backend did not restore every expected file")
    return {str(path.relative_to(work)): sha256(path) for path in files}


def record_result(work, result):
    write_json(work / "result.json", result)
    if os.environ.get("GITHUB_STEP_SUMMARY"):
        with open(os.environ["GITHUB_STEP_SUMMARY"], "a", encoding="utf-8") as stream:
            stream.write(f"Internal **{result['versionCode']} SERVED**, completed. Verified through a fresh deployed-state edit; lifecycle enum was not queried. Already served: {result['alreadyPublished']}.\n\nMain `{result['source']['sha']}`; signed AAB SHA-256 `{result['signedSha256']}`.\n")


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("command", choices=("produce", "gates", "prepare", "restore", "sign", "begin", "stage", "commit", "seal-intent", "verify-signed-cache", "verify-intent-cache", "park-signed-cache", "park-intent-cache"))
    args = parser.parse_args()
    source = source_env()
    root = Path(os.environ["GITHUB_WORKSPACE"])
    work = Path(os.environ.get("PLAY_WORK_DIR", str(Path(os.environ["RUNNER_TEMP"]) / "play")))
    github = GitHub()
    if args.command == "produce":
        require(os.environ.get("GITHUB_EVENT_NAME") == "push" and os.environ.get("GITHUB_REF") == "refs/heads/main", "Only main push produces release provenance")
        artifacts.produce(root, work / "unsigned", source)
    elif args.command == "gates":
        write_json(work / "gates/gates.json", {"schema": 1, "source": source, **{
            key: os.environ[env] for key, env in (("code", "CODE_CHANGED"), ("build", "BUILD_RESULT"),
                ("changes", "CHANGES_RESULT"), ("e2e", "E2E_RESULT"), ("instrumentation", "INSTRUMENTATION_RESULT"))}})
    elif args.command == "prepare":
        prepare(github, source, work)
        artifacts.validate_bundle(work / "unsigned", source, root)
        output("signed_name", artifact_name("signed", source))
        output("intent_name", artifact_name("intent", source) + f"-{int(os.environ['GITHUB_RUN_ID'])}-{int(os.environ['GITHUB_RUN_ATTEMPT'])}")
    elif args.command == "restore":
        github.fresh(source)
        restored = (work / "sealed/bundle.enc").exists()
        possible = github.prior_release_writes_possible(source, int(os.environ["GITHUB_RUN_ID"]), int(os.environ["GITHUB_RUN_ATTEMPT"]))
        require(restored or not possible, "Signed checkpoint is missing after possible Play writes; never regenerate different bytes for this version")
        if restored:
            artifacts.unseal(work / "sealed", work / "signed")
            validate_sealed(github, work, source, root)
            write_json(work / "cache-expected-signed.json", cache_snapshot(work, "signed"))
        intent = (work / "intent-sealed/intent.enc").exists()
        require(intent or not possible, "Edit checkpoint is missing after possible Play writes; preserve the occupied version and stop")
        require(not intent or restored, "Edit intent exists without sealed signed bytes; do not sign again")
        if intent:
            artifacts.unseal_intent(work)
        output("signed_restored", str(restored).lower())
    elif args.command == "sign":
        github.fresh(source)
        artifacts.validate_bundle(work / "unsigned", source, root)
        artifacts.sign(work / "unsigned", work / "signed")
        artifacts.validate_bundle(work / "signed", source, root, signed=True)
        artifacts.seal(work / "signed", work / "sealed", work / "unsigned", work / "gates/gates.json")
        write_json(work / "cache-expected-signed.json", cache_snapshot(work, "signed"))
    elif args.command.startswith("verify-"):
        kind = "signed" if args.command == "verify-signed-cache" else "intent"
        require(os.environ.get("CACHE_HIT") == "true", "Checkpoint was not independently restored from cache; no Play writes allowed")
        require(cache_snapshot(work, kind) == read_json(work / f"cache-expected-{kind}.json"), "Restored cache differs from the checkpoint being persisted")
        if kind == "signed":
            artifacts.unseal(work / "sealed", work / "signed")
            validate_sealed(github, work, source, root)
        else:
            artifacts.unseal_intent(work)
            manifest = validate_sealed(github, work, source, root)
            Publisher(None, manifest, lambda: None).validate_intent(read_json(work / "intent/intent.json"))
    elif args.command.startswith("park-"):
        kind = "signed" if args.command == "park-signed-cache" else "intent"
        require(cache_snapshot(work, kind) == read_json(work / f"cache-expected-{kind}.json"), "Local checkpoint changed before backend readback")
        phase = os.environ.get("CACHE_PHASE", "signed")
        require(phase in ("signed", "prepare", "commit"), "Invalid cache verification phase")
        directory = work / ("sealed" if kind == "signed" else "intent-sealed")
        backup = work / f"cache-local-{kind}-{phase}"
        require(not backup.exists(), "Checkpoint verification backup already exists")
        directory.rename(backup)
    elif args.command == "seal-intent":
        artifacts.seal_intent(work)
        write_json(work / "cache-expected-intent.json", cache_snapshot(work, "intent"))
    else:
        github.fresh(source)
        manifest = validate_sealed(github, work, source, root)
        publisher = Publisher(PlayApi(os.environ.get("PLAY_ACCESS_TOKEN")), manifest, lambda: github.fresh(source),
                              commit_started=lambda intent: github.commit_was_started(source, intent.get("writer")))
        if args.command == "begin":
            new_intent, done = publisher.begin(work / "intent/intent.json")
            output("new_intent", str(new_intent).lower())
            output("done", str(done).lower())
            if done:
                record_result(work, publisher.receipt(already=True))
                print(f"Internal {manifest['versionCode']} already served; deployed version and signed hash verified")
        else:
            intent = read_json(work / "intent/intent.json")
            if args.command == "stage":
                publisher.stage(intent, work / "signed/bundle.aab")
                intent["phase"] = "commit"
                intent["writer"] = {"run": int(os.environ["GITHUB_RUN_ID"]), "attempt": int(os.environ["GITHUB_RUN_ATTEMPT"])}
                write_json(work / "intent/intent.json", intent)
                return
            result = publisher.commit(intent)
            record_result(work, result)
            print(f"Internal {result['versionCode']} served (fresh deployed Internal completed + bundle hash)")


if __name__ == "__main__":
    try:
        main()
    except Stop as error:
        raise SystemExit(str(error)) from None

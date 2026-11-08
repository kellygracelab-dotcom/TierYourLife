"""Workflow entry points. Secrets are read only by sign / Play API commands."""
import argparse
import os
from pathlib import Path

from . import artifacts
from .github import GitHub, prepare
from .play import PlayApi, Publisher
from .policy import (Stop, artifact_name, output, read_json, require, source_env,
                     write_json)


def validate_sealed(github, work, source, root):
    original = read_json(work / "signed/provenance.json")["source"]
    require(original.get("sha") == source["sha"], "Sealed artifact is from another main SHA")
    unsigned = work / "unsigned"
    if original != source:
        directory = work / "original"
        github.verify_historical_ci(original, directory)
        unsigned = directory / "unsigned"
        artifacts.validate_bundle(unsigned, original, root)
    return artifacts.validate_bundle(work / "signed", original, root, signed=True, unsigned=unsigned / "bundle.aab")


def record_result(work, result):
    write_json(work / "result.json", result)
    if os.environ.get("GITHUB_STEP_SUMMARY"):
        with open(os.environ["GITHUB_STEP_SUMMARY"], "a", encoding="utf-8") as stream:
            stream.write(f"Internal **{result['versionCode']} PUBLISHED**, completed. Already published: {result['alreadyPublished']}.\n\nMain `{result['source']['sha']}`; signed AAB SHA-256 `{result['signedSha256']}`.\n")


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("command", choices=("produce", "gates", "prepare", "restore", "sign", "begin", "publish"))
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
        restored = github.restore_cd_artifact("signed", source, work / "sealed", {"bundle.enc", "provenance.json"})
        if restored:
            artifacts.unseal(work / "sealed", work / "signed")
            validate_sealed(github, work, source, root)
        intent = github.restore_cd_artifact("intent", source, work / "intent", {"intent.json"})
        require(not intent or restored, "Edit intent exists without sealed signed bytes; do not sign again")
        output("signed_restored", str(restored).lower())
    elif args.command == "sign":
        github.fresh(source)
        artifacts.validate_bundle(work / "unsigned", source, root)
        artifacts.sign(work / "unsigned", work / "signed")
        artifacts.validate_bundle(work / "signed", source, root, signed=True)
        artifacts.seal(work / "signed", work / "sealed")
    else:
        github.fresh(source)
        manifest = validate_sealed(github, work, source, root)
        publisher = Publisher(PlayApi(os.environ.get("PLAY_ACCESS_TOKEN")), manifest, lambda: github.fresh(source))
        if args.command == "begin":
            new_intent, done = publisher.begin(work / "intent/intent.json")
            output("new_intent", str(new_intent).lower())
            output("done", str(done).lower())
            if done:
                record_result(work, publisher.receipt(already=True))
                print(f"Internal {manifest['versionCode']} already PUBLISHED; signed hash verified")
        else:
            result = publisher.publish(read_json(work / "intent/intent.json"), work / "signed/bundle.aab")
            record_result(work, result)
            print(f"Internal {result['versionCode']} PUBLISHED (separate releases.list readback)")


if __name__ == "__main__":
    try:
        main()
    except Stop as error:
        raise SystemExit(str(error)) from None

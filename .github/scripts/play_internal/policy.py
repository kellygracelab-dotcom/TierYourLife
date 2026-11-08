"""Small, shared trust policy; no credentials or mutable release targets."""
import hashlib
import json
import os
from pathlib import Path
import re

REPO = "kellygracelab-dotcom/TierYourLife"
REPO_ID = 1279175743
OWNER_ID = 280666991
PACKAGE = "com.artiuillab.tieryourlife"
TRACK = "internal"
CERT_SHA256 = "D7FE40A522B6B0DEEF7BC5049EC22466C62941A808539B3D7F2C5B79F75D69C4"
PROXY_URL = "https://europe-west1-tieryourlife.cloudfunctions.net/"
CI_PATH = ".github/workflows/ci.yml"
CD_PATH = ".github/workflows/play-internal.yml"
BUNDLETOOL_URL = "https://github.com/google/bundletool/releases/download/1.18.3/bundletool-all-1.18.3.jar"
BUNDLETOOL_SHA256 = "a099cfa1543f55593bc2ed16a70a7c67fe54b1747bb7301f37fdfd6d91028e29"


class Stop(RuntimeError):
    """An expected, sanitized fail-closed outcome, safe to write in CI logs."""


def require(condition, message):
    if not condition:
        raise Stop(message)


def sha256(path):
    with Path(path).open("rb") as stream:
        return hashlib.file_digest(stream, "sha256").hexdigest() if hasattr(hashlib, "file_digest") else hashlib.sha256(stream.read()).hexdigest()


def read_json(path):
    return json.loads(Path(path).read_text(encoding="utf-8"))


def write_json(path, value):
    path = Path(path)
    path.parent.mkdir(parents=True, exist_ok=True)
    temporary = path.with_suffix(".tmp")
    temporary.write_text(json.dumps(value, indent=2, sort_keys=True) + "\n", encoding="utf-8")
    temporary.replace(path)


def output(name, value):
    require(re.fullmatch(r"[A-Za-z0-9_.-]+", str(value)) is not None, "Invalid workflow output")
    if os.environ.get("GITHUB_OUTPUT"):
        with open(os.environ["GITHUB_OUTPUT"], "a", encoding="utf-8") as stream:
            stream.write(f"{name}={value}\n")


def source_env():
    sha = os.environ["SOURCE_SHA"]
    require(re.fullmatch(r"[0-9a-f]{40}", sha) is not None, "Invalid source SHA")
    source = {"sha": sha, "runId": int(os.environ["SOURCE_RUN_ID"]), "attempt": int(os.environ["SOURCE_ATTEMPT"])}
    require(source["runId"] > 0 and source["attempt"] > 0, "Invalid CI run/attempt")
    return source


def artifact_name(kind, source):
    if kind in ("signed", "intent"):
        return f"play-{kind}-{source['sha']}"
    return f"play-{kind}-{source['runId']}-{source['attempt']}-{source['sha']}"


def validate_provenance(manifest, source):
    require(manifest.get("schema") == 1, "Unknown provenance schema")
    require(manifest.get("source") == source, "Artifact source differs from accepted CI")
    require(re.fullmatch(r"[0-9a-f]{40}", source.get("sha", "")) is not None
            and all(type(source.get(key)) is int and source[key] > 0 for key in ("runId", "attempt")), "Invalid CI identity")
    require(manifest.get("repositoryId") == REPO_ID and manifest.get("ownerId") == OWNER_ID, "Wrong artifact repository")
    require(manifest.get("event") == "push" and manifest.get("ref") == "refs/heads/main", "Artifact is not a main push")
    require(manifest.get("package") == PACKAGE and manifest.get("proxyUrl") == PROXY_URL, "Wrong release inputs")
    require(type(manifest.get("versionCode")) is int and 1 < manifest["versionCode"] <= 2100000000, "Invalid version code")
    for key in ("unsignedSha256", "firebaseConfigSha256"):
        require(re.fullmatch(r"[0-9a-f]{64}", manifest.get(key, "")) is not None, "Invalid provenance digest")

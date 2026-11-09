"""Build provenance, bounded data-only archives, manifest and signature checks."""
import base64
import hashlib
import json
import os
from pathlib import Path
import re
import shutil
import stat
import subprocess
import tempfile
import urllib.request
import xml.etree.ElementTree as ET
import zipfile

from .policy import (BUNDLETOOL_SHA256, BUNDLETOOL_URL, CERT_SHA256, OWNER_ID,
                     PACKAGE, PROXY_URL, REPO_ID, Stop, read_json, require,
                     sha256, validate_provenance, write_json)


def command(args, *, env=None):
    result = subprocess.run(args, capture_output=True, text=True, env=env)
    require(result.returncode == 0, f"Release tool failed: {Path(args[0]).name}")
    return result.stdout.strip()


def java_tool(name):
    suffix = ".exe" if os.name == "nt" else ""
    return str(Path(os.environ["JAVA_HOME"]) / "bin" / (name + suffix))


def bundletool(directory):
    target = Path(directory) / "bundletool.jar"
    if not target.exists():
        with urllib.request.urlopen(BUNDLETOOL_URL, timeout=60) as response:
            data = response.read(40_000_001)
        require(len(data) <= 40_000_000, "Bundletool download too large")
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_bytes(data)
    require(sha256(target) == BUNDLETOOL_SHA256, "Bundletool digest mismatch")
    return target


def verify_signature(bundle, signed):
    helper = Path(__file__).with_name("VerifyBundle.java")
    command([java_tool("java"), str(helper), str(bundle), CERT_SHA256 if signed else "unsigned"])


def compare_payloads(unsigned, signed):
    def payloads(path):
        with zipfile.ZipFile(path) as archive:
            return {item.filename: hashlib.sha256(archive.read(item)).hexdigest()
                    for item in archive.infolist() if not item.is_dir() and not re.fullmatch(
                        r"META-INF/(MANIFEST\.MF|[^/]+\.(SF|RSA|DSA|EC)|SIG-[^/]+)", item.filename.upper())}
    require(payloads(unsigned) == payloads(signed), "Signed payload differs from the accepted CI AAB")


def checkpoint_cipher(mode, source, destination):
    require(bool(os.environ.get("PLAY_CHECKPOINT_KEY")), "Checkpoint encryption secret PLAY_CHECKPOINT_KEY is missing")
    helper = Path(__file__).with_name("CheckpointCipher.java")
    command([java_tool("java"), str(helper), mode, str(source), str(destination)])


def seal(signed, sealed, unsigned, gates):
    signed, sealed, unsigned = Path(signed), Path(sealed), Path(unsigned)
    sealed.mkdir(parents=True, exist_ok=True)
    manifest = read_json(signed / "provenance.json")
    manifest["encryption"] = "AES-256-GCM-checkpoint-v2"
    archive = sealed.parent / "checkpoint.zip"
    try:
        with zipfile.ZipFile(archive, "w") as zipped:
            zipped.write(signed / "bundle.aab", "signed.aab")
            zipped.write(unsigned / "bundle.aab", "unsigned.aab")
            zipped.writestr("provenance.json", json.dumps(manifest))
            zipped.write(gates, "gates.json")
        checkpoint_cipher("encrypt", archive, sealed / "bundle.enc")
    finally:
        archive.unlink(missing_ok=True)
    write_json(sealed / "provenance.json", manifest)


def unseal(sealed, signed):
    sealed, signed = Path(sealed), Path(signed)
    manifest = read_json(sealed / "provenance.json")
    require(manifest.get("encryption") == "AES-256-GCM-checkpoint-v2", "Unexpected checkpoint encryption")
    signed.mkdir(parents=True, exist_ok=True)
    original = signed.parent / "original"
    archive = signed.parent / "checkpoint-restored.zip"
    try:
        checkpoint_cipher("decrypt", sealed / "bundle.enc", archive)
        extract_data(archive, original, {"signed.aab", "unsigned.aab", "provenance.json", "gates.json"})
    finally:
        archive.unlink(missing_ok=True)
    require(read_json(original / "provenance.json") == manifest, "Unauthenticated checkpoint metadata differs from encrypted record")
    shutil.copyfile(original / "signed.aab", signed / "bundle.aab")
    (original / "unsigned").mkdir(exist_ok=True)
    shutil.copyfile(original / "unsigned.aab", original / "unsigned/bundle.aab")
    write_json(original / "unsigned/provenance.json", manifest)
    (original / "gates").mkdir(exist_ok=True)
    shutil.copyfile(original / "gates.json", original / "gates/gates.json")
    require(sha256(signed / "bundle.aab") == manifest.get("signedSha256"), "Decrypted checkpoint hash mismatch")
    write_json(signed / "provenance.json", manifest)


def seal_intent(work):
    work = Path(work)
    target = work / "intent-sealed/intent.enc"
    target.parent.mkdir(parents=True, exist_ok=True)
    checkpoint_cipher("encrypt", work / "intent/intent.json", target)


def unseal_intent(work):
    work = Path(work)
    (work / "intent").mkdir(parents=True, exist_ok=True)
    checkpoint_cipher("decrypt", work / "intent-sealed/intent.enc", work / "intent/intent.json")


def verify_manifest(bundle, version, tools):
    jar = str(bundletool(tools))
    prefix = [java_tool("java"), "-jar", jar]
    command(prefix + ["validate", f"--bundle={bundle}"])
    manifest = ET.fromstring(command(prefix + ["dump", "manifest", f"--bundle={bundle}", "--module=base"]))
    android = "{http://schemas.android.com/apk/res/android}"
    require(manifest.get("package") == PACKAGE, "Wrong bundle package")
    require(manifest.get(android + "versionCode") == str(version), "Bundle version differs from full-history commit count")
    app = manifest.find("application")
    require(app is not None and app.get(android + "debuggable", "false") == "false" and app.get(android + "testOnly", "false") == "false", "Test/debug bundle rejected")


def public_firebase_digest(root):
    path = root / "app/google-services.json"
    config = read_json(path)
    require(config.get("project_info", {}).get("project_id") == "tieryourlife", "Wrong Firebase project")
    clients = [client for client in config.get("client", []) if client.get("client_info", {}).get("android_client_info", {}).get("package_name") == PACKAGE]
    require(len(clients) == 1 and any(c.get("client_type") == 3 for c in clients[0].get("oauth_client", [])), "Missing release Google sign-in configuration")
    return sha256(path)


def produce(root, destination, source):
    root, destination = Path(root), Path(destination)
    require(command(["git", "-C", str(root), "rev-parse", "--is-shallow-repository"]) == "false", "Shallow release history")
    require(command(["git", "-C", str(root), "rev-parse", "HEAD"]) == source["sha"], "Checkout is not the source SHA")
    require(not command(["git", "-C", str(root), "status", "--porcelain", "--untracked-files=no"]), "Dirty release source")
    version = int(command(["git", "-C", str(root), "rev-list", "--count", "HEAD"]))
    bundle = root / "app/build/outputs/bundle/release/app-release.aab"
    verify_signature(bundle, False)
    verify_manifest(bundle, version, destination.parent / "tools")
    destination.mkdir(parents=True, exist_ok=True)
    shutil.copyfile(bundle, destination / "bundle.aab")
    write_json(destination / "provenance.json", {
        "schema": 1, "source": source, "tree": command(["git", "-C", str(root), "rev-parse", "HEAD^{tree}"]),
        "repositoryId": REPO_ID, "ownerId": OWNER_ID, "event": "push", "ref": "refs/heads/main",
        "package": PACKAGE, "proxyUrl": PROXY_URL, "versionCode": version,
        "firebaseConfigSha256": public_firebase_digest(root), "unsignedSha256": sha256(bundle),
    })


def extract_data(archive, destination, allowed):
    destination = Path(destination)
    with zipfile.ZipFile(archive) as zipped:
        entries = zipped.infolist()
        require(len(entries) == len(allowed) and {item.filename for item in entries} == set(allowed), "Artifact contains unexpected or duplicate files")
        require(sum(item.file_size for item in entries) <= 150_000_000, "Artifact exceeds size limit")
        for item in entries:
            mode = item.external_attr >> 16
            require(not stat.S_ISLNK(mode) and not item.is_dir() and not item.flag_bits & 1, "Unsupported artifact entry")
            limit = 140_000_000 if item.filename.endswith(".aab") else 32_000
            require(item.file_size <= limit, "Artifact entry too large")
            destination.mkdir(parents=True, exist_ok=True)
            (destination / item.filename).write_bytes(zipped.read(item))


def validate_bundle(directory, source, root, *, signed=False, unsigned=None):
    directory, root = Path(directory), Path(root)
    manifest = read_json(directory / "provenance.json")
    validate_provenance(manifest, source)
    require(manifest["firebaseConfigSha256"] == public_firebase_digest(root), "Firebase configuration differs from source")
    require(manifest["tree"] == command(["git", "-C", str(root), "rev-parse", "HEAD^{tree}"]), "Wrong source tree")
    require(manifest["versionCode"] == int(command(["git", "-C", str(root), "rev-list", "--count", "HEAD"])), "Wrong source version")
    bundle = directory / "bundle.aab"
    require(sha256(bundle) == manifest.get("signedSha256" if signed else "unsignedSha256"), "Bundle digest mismatch")
    if signed:
        require(manifest.get("uploadCertificateSha256") == CERT_SHA256, "Unexpected signing provenance")
        compare_payloads(Path(unsigned) if unsigned is not None else directory.parent / "unsigned/bundle.aab", bundle)
    verify_signature(bundle, signed)
    verify_manifest(bundle, manifest["versionCode"], directory.parent / "tools")
    return manifest


def sign(unsigned, signed):
    unsigned, signed = Path(unsigned), Path(signed)
    names = ["PLAY_UPLOAD_KEYSTORE_BASE64", "PLAY_UPLOAD_STORE_PASSWORD", "PLAY_UPLOAD_KEY_ALIAS", "PLAY_UPLOAD_KEY_PASSWORD"]
    require(all(os.environ.get(name) for name in names), "Signing secrets are missing")
    require(bool(os.environ.get("PLAY_CHECKPOINT_KEY")), "Checkpoint encryption secret PLAY_CHECKPOINT_KEY is missing")
    alias = os.environ[names[2]]
    require(re.fullmatch(r"[A-Za-z0-9_][A-Za-z0-9_.-]{0,127}", alias) is not None, "Unsupported signing alias")
    signed.mkdir(parents=True, exist_ok=True)
    # Only this exact temporary directory holds the private key. Never upload it.
    with tempfile.TemporaryDirectory(prefix="play-key-", dir=os.environ.get("RUNNER_TEMP")) as sensitive:
        os.chmod(sensitive, 0o700)
        key = Path(sensitive) / "upload.keystore"
        try:
            try:
                data = base64.b64decode(os.environ[names[0]], validate=True)
            except ValueError:
                raise Stop("Invalid encoded signing key") from None
            require(0 < len(data) <= 1_000_000, "Invalid signing key size")
            with key.open("xb") as stream:
                os.chmod(key, 0o600)
                stream.write(data)
            del data
            child_env = {key: value for key, value in os.environ.items() if key not in (names[0], names[2])}
            command([java_tool("jarsigner"), "-keystore", str(key), "-storepass:env", names[1], "-keypass:env", names[3],
                     "-digestalg", "SHA-256", "-sigfile", "UPLOAD", "-signedjar", str(signed / "bundle.aab"), str(unsigned / "bundle.aab"), alias], env=child_env)
        finally:
            key.unlink(missing_ok=True)
    verify_signature(signed / "bundle.aab", True)
    manifest = read_json(unsigned / "provenance.json")
    manifest["signedSha256"] = sha256(signed / "bundle.aab")
    manifest["uploadCertificateSha256"] = CERT_SHA256
    write_json(signed / "provenance.json", manifest)

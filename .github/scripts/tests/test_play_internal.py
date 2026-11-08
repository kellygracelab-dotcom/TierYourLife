"""Release safety regressions. No real credentials, Play writes or network."""
import copy
import base64
import hashlib
import io
import json
import os
from pathlib import Path
import sys
import tempfile
import shutil
import unittest
from unittest.mock import patch
import zipfile

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from play_internal.artifacts import checkpoint_cipher, command, compare_payloads, extract_data, java_tool, sign, verify_signature
from play_internal.__main__ import validate_sealed
from play_internal.github import GitHub, verify_gates
from play_internal.play import ApiError, PUBLISHED, Publisher
from play_internal.policy import (CD_PATH, CI_PATH, OWNER_ID, PACKAGE, PROXY_URL,
                                  REPO_ID, Stop, artifact_name, validate_provenance,
                                  write_json)

SOURCE = {"sha": "a" * 40, "runId": 123, "attempt": 2}
MANIFEST = {"schema": 1, "source": SOURCE, "repositoryId": REPO_ID, "ownerId": OWNER_ID,
            "event": "push", "ref": "refs/heads/main", "package": PACKAGE,
            "proxyUrl": PROXY_URL, "versionCode": 479, "unsignedSha256": "b" * 64,
            "firebaseConfigSha256": "c" * 64, "signedSha256": "d" * 64}


def run(cd=False):
    repo = {"id": REPO_ID, "owner": {"id": OWNER_ID}}
    return {"path": CD_PATH if cd else CI_PATH, "event": "workflow_run" if cd else "push",
            "head_branch": "main", "head_sha": SOURCE["sha"], "repository": repo,
            "head_repository": copy.deepcopy(repo), "name": "CI", "run_attempt": 2,
            "conclusion": "success", "status": "completed"}


def gates(code="true"):
    receipt = {"schema": 1, "source": SOURCE, "code": code, "build": "success", "changes": "success",
               "e2e": "success" if code == "true" else "skipped", "instrumentation": "success" if code == "true" else "skipped"}
    names = ["Build, unit tests, lint", "What changed", "Android CI passed"]
    if code == "true":
        names += ["End-to-end against the proxy", "Instrumentation tests (26)", "Instrumentation tests (34)"]
    return [{"name": name, "conclusion": "success"} for name in names], receipt


class TrustTests(unittest.TestCase):
    def test_fork_pr_dispatch_and_wrong_workflow_never_trusted(self):
        for key, value in (("event", "pull_request"), ("event", "workflow_dispatch"),
                           ("path", CD_PATH), ("head_branch", "topic"), ("head_sha", "0" * 40)):
            with self.subTest(key=key, value=value), self.assertRaises(Stop):
                data = run()
                data[key] = value
                GitHub().trusted_run(data, SOURCE)
        for field in ("repository", "head_repository"):
            for key in ("id", "owner"):
                data = run()
                data[field][key] = 9 if key == "id" else {"id": 9}
                with self.subTest(field=field, key=key), self.assertRaises(Stop):
                    GitHub().trusted_run(data, SOURCE)

    def test_current_main_and_latest_successful_attempt_required(self):
        github = GitHub()
        for sha, attempt, conclusion in (("0" * 40, 2, "success"), (SOURCE["sha"], 3, "success"), (SOURCE["sha"], 2, "failure")):
            data = run()
            data.update(run_attempt=attempt, conclusion=conclusion)
            with patch.object(github, "get", side_effect=[{"object": {"sha": sha}}, data]), self.assertRaises(Stop):
                github.fresh(SOURCE)

    def test_code_requires_both_instrumentation_devices_and_e2e(self):
        jobs, receipt = gates()
        verify_gates(jobs, receipt, SOURCE)
        for name in ("Instrumentation tests (26)", "Instrumentation tests (34)", "End-to-end against the proxy", "Android CI passed"):
            for result in ("failure", "cancelled", "skipped", "timed_out"):
                broken = copy.deepcopy(jobs)
                next(item for item in broken if item["name"] == name)["conclusion"] = result
                with self.subTest(name=name, result=result), self.assertRaises(Stop):
                    verify_gates(broken, receipt, SOURCE)

    def test_docs_only_skip_needs_explicit_gate_receipt(self):
        jobs, receipt = gates("false")
        verify_gates(jobs, receipt, SOURCE)
        receipt["code"] = "true"
        with self.assertRaises(Stop):
            verify_gates(jobs, receipt, SOURCE)

    def test_failed_or_wrong_attempt_receipt_rejected(self):
        for key, value in (("build", "failure"), ("changes", "skipped"), ("code", "unknown"), ("source", {**SOURCE, "attempt": 1})):
            jobs, receipt = gates()
            receipt[key] = value
            with self.subTest(key=key), self.assertRaises(Stop):
                verify_gates(jobs, receipt, SOURCE)

    def test_release_provenance_rejects_placeholder_inputs(self):
        validate_provenance(MANIFEST, SOURCE)
        for key, value in (("proxyUrl", ""), ("proxyUrl", "https://example.invalid/"), ("package", "wrong"),
                           ("event", "pull_request"), ("repositoryId", 3), ("versionCode", 1), ("versionCode", "479"),
                           ("unsignedSha256", "invalid"), ("source", {**SOURCE, "attempt": 3})):
            changed = {**MANIFEST, key: value}
            with self.subTest(key=key, value=value), self.assertRaises(Stop):
                validate_provenance(changed, SOURCE)

    def test_unsigned_names_bind_attempt_and_sealed_names_bind_source(self):
        self.assertNotEqual(artifact_name("unsigned", SOURCE), artifact_name("unsigned", {**SOURCE, "attempt": 3}))
        self.assertEqual(artifact_name("signed", SOURCE), artifact_name("signed", {**SOURCE, "attempt": 3}))
        self.assertNotEqual(artifact_name("signed", SOURCE), artifact_name("signed", {**SOURCE, "sha": "0" * 40}))

    def test_pr_cannot_forge_a_durable_signed_artifact(self):
        github = GitHub()
        artifact = {"id": 8, "name": artifact_name("signed", SOURCE), "workflow_run": {"id": 10}}
        with patch.object(github, "pages", return_value=[artifact]), patch.object(github, "get", return_value=run()), patch.object(github, "download") as download:
            self.assertFalse(github.restore_cd_artifact("signed", SOURCE, "unused", {"bundle.aab", "provenance.json"}))
            download.assert_not_called()

    def test_cd_conflicting_artifacts_are_not_chosen_arbitrarily(self):
        github = GitHub()
        artifact = {"id": 8, "name": artifact_name("signed", SOURCE), "workflow_run": {"id": 10}}
        with patch.object(github, "pages", return_value=[artifact, artifact]), patch.object(github, "get", return_value=run(True)), self.assertRaises(Stop):
            github.restore_cd_artifact("signed", SOURCE, "unused", {"bundle.aab", "provenance.json"})

    def test_same_sha_ci_rerun_verifies_original_attempt_instead_of_resigning(self):
        github = unittest.mock.Mock()
        newer = {**SOURCE, "attempt": 3}
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            write_json(root / "signed/provenance.json", MANIFEST)
            with patch("play_internal.artifacts.validate_bundle", return_value=MANIFEST) as validate:
                result = validate_sealed(github, root, newer, root)
            self.assertEqual(result, MANIFEST)
            github.verify_historical_ci.assert_called_once_with(SOURCE, root / "original")
            self.assertEqual(validate.call_args.args[1], SOURCE)
            self.assertEqual(validate.call_args.kwargs["unsigned"], root / "original/unsigned/bundle.aab")

    def test_latest_trusted_intent_generation_is_restored(self):
        github = GitHub()
        name = artifact_name("intent", SOURCE)
        artifacts = [{"id": item, "name": f"{name}-10-{item}", "workflow_run": {"id": 10}} for item in (8, 9)]
        with patch.object(github, "pages", return_value=artifacts), patch.object(github, "get", return_value=run(True)), patch.object(github, "download") as download:
            self.assertTrue(github.restore_cd_artifact("intent", SOURCE, "unused", {"intent.json"}))
            self.assertEqual(download.call_args.args[0]["id"], 9)

    def test_expired_or_hash_mismatched_archive_rejected(self):
        github = GitHub()
        for overrides in ({"expired": True}, {"digest": "sha256:" + "0" * 64}):
            artifact = {"id": 1, "expired": False, "size_in_bytes": 3, "digest": "sha256:" + hashlib.sha256(b"bad").hexdigest(), **overrides}
            with patch.object(github, "raw", return_value=b"bad"), self.assertRaises(Stop):
                github.download(artifact, "unused", {"bundle.aab"})


class ArchiveTests(unittest.TestCase):
    def archive(self, entries):
        memory = io.BytesIO()
        with zipfile.ZipFile(memory, "w") as zipped:
            for name, content in entries:
                zipped.writestr(name, content)
        memory.seek(0)
        return memory

    def test_exact_data_only_archive(self):
        with tempfile.TemporaryDirectory() as directory:
            extract_data(self.archive([("bundle.aab", b"aab"), ("provenance.json", b"{}")]), directory, {"bundle.aab", "provenance.json"})
            self.assertEqual((Path(directory) / "bundle.aab").read_bytes(), b"aab")

    def test_traversal_extra_file_and_symlink_rejected(self):
        link = zipfile.ZipInfo("bundle.aab")
        link.external_attr = 0o120777 << 16
        link.create_system = 3
        for entries in ([ ("../bundle.aab", b"x") ], [("script.py", b"x")], [(link, b"target")]):
            with tempfile.TemporaryDirectory() as directory, self.assertRaises(Stop):
                extract_data(self.archive(entries), directory, {"bundle.aab"})

    def test_signing_error_never_echoes_private_encoded_key(self):
        secrets = {"PLAY_UPLOAD_KEYSTORE_BASE64": "private-invalid-value", "PLAY_UPLOAD_STORE_PASSWORD": "password",
                   "PLAY_UPLOAD_KEY_ALIAS": "upload", "PLAY_UPLOAD_KEY_PASSWORD": "other-password", "PLAY_CHECKPOINT_KEY": "synthetic"}
        with tempfile.TemporaryDirectory() as directory, patch.dict(os.environ, secrets), self.assertRaises(Stop) as caught:
            sign(directory, Path(directory) / "signed")
        self.assertNotIn("private-invalid", str(caught.exception))

    def test_signed_payload_must_still_be_exact_ci_payload(self):
        with tempfile.TemporaryDirectory() as directory:
            unsigned, signed = Path(directory) / "unsigned.zip", Path(directory) / "signed.zip"
            unsigned.write_bytes(self.archive([("base/dex/classes.dex", b"original")]).read())
            signed.write_bytes(self.archive([("base/dex/classes.dex", b"changed"), ("META-INF/UPLOAD.SF", b"signature")]).read())
            with self.assertRaises(Stop):
                compare_payloads(unsigned, signed)


@unittest.skipUnless(os.environ.get("JAVA_HOME"), "JDK required for real JAR signature tests")
class SignatureTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.temp = tempfile.TemporaryDirectory()
        cls.root = Path(cls.temp.name)
        cls.helper = str(Path(__file__).resolve().parents[1] / "play_internal/VerifyBundle.java")
        cls.unsigned = cls.root / "unsigned.aab"
        with zipfile.ZipFile(cls.unsigned, "w") as archive:
            archive.writestr("base/dex/classes.dex", b"only a synthetic fixture")
        # Generated test key only. No personal keystore/configuration is read.
        env = {**os.environ, "TEST_JAR_PASSWORD": "synthetic-test-password"}
        command([java_tool("keytool"), "-genkeypair", "-alias", "fixture", "-keystore", str(cls.root / "fixture.p12"),
                 "-storepass:env", "TEST_JAR_PASSWORD", "-keypass:env", "TEST_JAR_PASSWORD", "-keyalg", "RSA",
                 "-validity", "2", "-dname", "CN=Synthetic CI Fixture", "-noprompt"], env=env)
        command([java_tool("keytool"), "-exportcert", "-alias", "fixture", "-keystore", str(cls.root / "fixture.p12"),
                 "-storepass:env", "TEST_JAR_PASSWORD", "-file", str(cls.root / "cert.der")], env=env)
        cls.fingerprint = hashlib.sha256((cls.root / "cert.der").read_bytes()).hexdigest()
        cls.signed = cls.root / "signed.aab"
        command([java_tool("jarsigner"), "-keystore", str(cls.root / "fixture.p12"), "-storepass:env", "TEST_JAR_PASSWORD",
                 "-keypass:env", "TEST_JAR_PASSWORD", "-signedjar", str(cls.signed), str(cls.unsigned), "fixture"], env=env)

    @classmethod
    def tearDownClass(cls):
        cls.temp.cleanup()

    def test_unsigned_and_valid_signed_fixture_are_distinguished(self):
        verify_signature(self.unsigned, False)
        command([java_tool("java"), self.helper, str(self.signed), self.fingerprint])
        with self.assertRaises(Stop):
            verify_signature(self.signed, False)
        with self.assertRaises(Stop):
            verify_signature(self.unsigned, True)

    def test_fake_certificate_cannot_pass_real_upload_policy(self):
        with self.assertRaises(Stop):
            verify_signature(self.signed, True)

    def test_added_unsigned_entry_rejected_despite_valid_original_signature(self):
        injected = self.root / "injected.aab"
        shutil.copyfile(self.signed, injected)
        with zipfile.ZipFile(injected, "a") as archive:
            archive.writestr("base/assets/injected.txt", b"unsigned payload")
        with self.assertRaises(Stop):
            command([java_tool("java"), self.helper, str(injected), self.fingerprint])

    def test_checkpoint_roundtrip_retains_exact_signed_bytes(self):
        encrypted, restored = self.root / "roundtrip.enc", self.root / "roundtrip.aab"
        key = base64.b64encode(bytes(range(32))).decode()
        with patch.dict(os.environ, {"PLAY_CHECKPOINT_KEY": key}):
            checkpoint_cipher("encrypt", self.signed, encrypted)
            checkpoint_cipher("decrypt", encrypted, restored)
        self.assertEqual(self.signed.read_bytes(), restored.read_bytes())
        self.assertNotEqual(encrypted.read_bytes()[:2], b"PK")

    def test_checkpoint_tamper_never_writes_plaintext(self):
        encrypted, restored = self.root / "tampered.enc", self.root / "tampered.aab"
        key = base64.b64encode(bytes(range(32))).decode()
        with patch.dict(os.environ, {"PLAY_CHECKPOINT_KEY": key}):
            checkpoint_cipher("encrypt", self.signed, encrypted)
            data = bytearray(encrypted.read_bytes())
            data[-1] ^= 1
            encrypted.write_bytes(data)
            with self.assertRaises(Stop):
                checkpoint_cipher("decrypt", encrypted, restored)
        self.assertFalse(restored.exists())

    def test_checkpoint_wrong_key_cannot_decrypt(self):
        encrypted, restored = self.root / "wrong-key.enc", self.root / "wrong-key.aab"
        with patch.dict(os.environ, {"PLAY_CHECKPOINT_KEY": base64.b64encode(bytes(range(32))).decode()}):
            checkpoint_cipher("encrypt", self.signed, encrypted)
        with patch.dict(os.environ, {"PLAY_CHECKPOINT_KEY": base64.b64encode(bytes(reversed(range(32)))).decode()}), self.assertRaises(Stop):
            checkpoint_cipher("decrypt", encrypted, restored)
        self.assertFalse(restored.exists())


class FakePlay:
    def __init__(self):
        self.calls = []
        self.bundles = [{"versionCode": 477, "sha256": "e" * 64}]
        self.track = {"track": "internal", "releases": [{"name": "old", "versionCodes": ["477"], "status": "completed"}]}
        self.summary = {"internal": [{"track": "internal", "releaseName": "old", "releaseLifecycleState": PUBLISHED, "activeArtifacts": [{"versionCode": "477"}]}], "production": []}
        self.missing_internal = False
        self.upload_error = self.commit_error = self.put_error = False
        self.upload_consumed = True
        self.expired = False
        self.commit_publishes = True

    def request(self, method, path, body=None, upload=False):
        self.calls.append((method, path, copy.deepcopy(body) if not upload else "bundle"))
        if method == "GET":
            if path.startswith("/tracks/"):
                return {"releases": copy.deepcopy(self.summary[path.split("/")[2]])}
            if path.endswith("/bundles"):
                return {"bundles": copy.deepcopy(self.bundles)}
            if path.endswith("/apks"):
                return {"apks": []}
            if path.endswith("/tracks"):
                return {"tracks": ([{"track": "production"}] if self.missing_internal else [copy.deepcopy(self.track), {"track": "production"}])}
            if path.endswith("/tracks/internal"):
                return copy.deepcopy(self.track)
            if self.expired:
                raise ApiError(404)
            return {"id": "edit1"}
        if path == "/edits":
            return {"id": "edit1"}
        if "/bundles?" in path:
            candidate = {"versionCode": 479, "sha256": MANIFEST["signedSha256"]}
            if not self.upload_error or self.upload_consumed:
                self.bundles.append(candidate)
            if self.upload_error:
                raise ApiError(503)
            return candidate
        if method == "PUT":
            self.track = copy.deepcopy(body)
            if self.put_error:
                raise ApiError(503)
            return body
        if ":commit?" in path:
            if self.commit_publishes:
                self.summary["internal"] = [{"track": "internal", "releaseName": body_name(self.track), "releaseLifecycleState": PUBLISHED,
                                             "activeArtifacts": [{"versionCode": "479"}]}]
            if self.commit_error:
                raise ApiError(503)
            return {"id": "edit1"}
        raise AssertionError((method, path))


def body_name(track):
    return track["releases"][0]["name"]


class PublisherTests(unittest.TestCase):
    def setUp(self):
        self.api = FakePlay()
        self.fresh = unittest.mock.Mock()
        self.publisher = Publisher(self.api, copy.deepcopy(MANIFEST), self.fresh, sleep=lambda _: None)
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.intent_path = Path(self.temp.name) / "intent.json"

    def begin(self):
        self.assertEqual(self.publisher.begin(self.intent_path), (True, False))
        return json.loads(self.intent_path.read_text())

    def mutations(self, kind):
        return [call for call in self.api.calls if call[0] != "GET" and kind in call[1]]

    def test_success_writes_only_internal_and_safe_review_commit(self):
        intent = self.begin()
        self.assertEqual(self.mutations("/bundles"), [])  # intent checkpoint precedes upload
        result = self.publisher.publish(intent, "fake.aab")
        self.assertEqual(result["outcome"], "PUBLISHED")
        self.assertEqual(len(self.mutations("/bundles")), 1)
        self.assertEqual([call[1] for call in self.mutations("/tracks")], ["/edits/edit1/tracks/internal"])
        self.assertEqual(self.mutations(":commit")[0][1], "/edits/edit1:commit?changesInReviewBehavior=ERROR_IF_IN_REVIEW&changesNotSentForReview=true")
        self.assertGreaterEqual(self.fresh.call_count, 6)

    def test_pending_production_or_internal_never_uploads(self):
        for track in ("internal", "production"):
            for state in ("DRAFT", "NOT_SENT_FOR_REVIEW", "IN_REVIEW", "APPROVED_NOT_PUBLISHED", "NOT_APPROVED", "UNSPECIFIED"):
                self.api.summary[track] = [{"track": track, "releaseLifecycleState": "RELEASE_LIFECYCLE_STATE_" + state}]
                with self.subTest(track=track, state=state), self.assertRaises(Stop):
                    self.publisher.begin(self.intent_path)
                self.assertEqual(self.mutations("/bundles"), [])
                self.api.summary[track] = []

    def test_missing_internal_does_not_create_or_fallback_track(self):
        self.api.missing_internal = True
        with self.assertRaises(Stop):
            self.publisher.begin(self.intent_path)
        self.assertEqual(self.mutations("/tracks"), [])

    def test_occupied_different_bytes_and_newer_version_block(self):
        for code, digest in ((479, "wrong"), (480, "d" * 64)):
            self.api.bundles = [{"versionCode": code, "sha256": digest}]
            with self.subTest(code=code), self.assertRaises(Stop):
                self.publisher.begin(self.intent_path)
            self.assertEqual(self.mutations("/bundles"), [])

    def test_existing_own_edit_reuses_same_uploaded_bytes(self):
        intent = self.begin()
        self.api.bundles.append({"versionCode": 479, "sha256": MANIFEST["signedSha256"]})
        self.assertEqual(self.publisher.begin(self.intent_path), (False, False))
        self.publisher.publish(intent, "fake.aab")
        self.assertEqual(self.mutations("/bundles"), [])

    def test_lost_upload_response_reconciles_without_second_upload(self):
        intent = self.begin()
        self.api.upload_error = True
        result = self.publisher.publish(intent, "fake.aab")
        self.assertEqual(result["outcome"], "PUBLISHED")
        self.assertEqual(len(self.mutations("/bundles")), 1)

    def test_unknown_upload_outcome_does_not_update_or_commit(self):
        intent = self.begin()
        self.api.upload_error, self.api.upload_consumed = True, False
        with self.assertRaises(Stop):
            self.publisher.publish(intent, "fake.aab")
        self.assertEqual(len(self.mutations("/bundles")), 1)
        self.assertEqual(self.mutations("/tracks"), [])
        self.assertEqual(self.mutations(":commit"), [])

    def test_lost_track_response_reads_back_without_second_put(self):
        intent = self.begin()
        self.api.put_error = True
        self.publisher.publish(intent, "fake.aab")
        self.assertEqual(len(self.mutations("/tracks")), 1)

    def test_lost_commit_response_proves_published_without_second_commit(self):
        intent = self.begin()
        self.api.commit_error = True
        result = self.publisher.publish(intent, "fake.aab")
        self.assertEqual(result["outcome"], "PUBLISHED")
        self.assertEqual(len(self.mutations(":commit")), 1)

    def test_successful_commit_alone_is_not_publication(self):
        intent = self.begin()
        self.api.commit_publishes = False
        with self.assertRaisesRegex(Stop, "availability not proven"):
            self.publisher.publish(intent, "fake.aab")

    def test_review_error_never_retries_with_cancellation(self):
        intent = self.begin()
        self.api.commit_error, self.api.commit_publishes = True, False
        with self.assertRaises(Stop):
            self.publisher.publish(intent, "fake.aab")
        self.assertEqual(len(self.mutations(":commit")), 1)

    def test_expired_unused_intent_can_resume_after_readonly_free_code_check(self):
        first = self.begin()
        self.api.expired = True
        self.assertEqual(self.publisher.begin(self.intent_path), (True, False))
        self.assertEqual(len(self.mutations("/edits")), 2)
        self.assertEqual(self.mutations("/bundles"), [])
        self.assertEqual(json.loads(self.intent_path.read_text())["signedSha256"], first["signedSha256"])

    def test_expired_intent_with_foreign_occupied_code_stops(self):
        self.begin()
        self.api.expired = True
        self.api.bundles.append({"versionCode": 479, "sha256": "foreign"})
        with self.assertRaises(Stop):
            self.publisher.begin(self.intent_path)
        self.assertEqual(self.mutations("/bundles"), [])

    def test_own_pending_release_is_polled_without_repeating_writes(self):
        intent = self.begin()
        original = self.publisher.published
        count = 0
        def pending_then_published():
            nonlocal count
            count += 1
            if count <= 2:
                self.api.summary["internal"][0]["releaseLifecycleState"] = "RELEASE_LIFECYCLE_STATE_IN_REVIEW"
            else:
                self.api.summary["internal"][0]["releaseLifecycleState"] = PUBLISHED
            return original()
        with patch.object(self.publisher, "published", side_effect=pending_then_published):
            result = self.publisher.publish(intent, "fake.aab")
        self.assertEqual(result["outcome"], "PUBLISHED")
        self.assertGreaterEqual(count, 3)
        self.assertEqual(len(self.mutations("/bundles")), 1)
        self.assertEqual(len(self.mutations(":commit")), 1)

    def test_expired_committed_edit_requires_published_and_hash_proof(self):
        intent = self.begin()
        self.publisher.publish(intent, "fake.aab")
        self.api.expired = True
        self.assertEqual(self.publisher.begin(self.intent_path), (False, True))
        self.assertEqual(len(self.mutations("/bundles")), 1)

    def test_intent_tampering_and_stale_main_stop_before_upload(self):
        intent = self.begin()
        for key, value in (("track", "production"), ("signedSha256", "wrong"), ("editId", "../other")):
            with self.subTest(key=key), self.assertRaises(Stop):
                self.publisher.publish({**intent, key: value}, "fake.aab")
        self.fresh.side_effect = Stop("stale")
        with self.assertRaises(Stop):
            self.publisher.publish(intent, "fake.aab")
        self.assertEqual(self.mutations("/bundles"), [])


if __name__ == "__main__":
    unittest.main()

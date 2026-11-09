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
from play_internal.artifacts import checkpoint_cipher, command, compare_payloads, extract_data, java_tool, sign, verify_signature, seal, unseal, seal_intent, unseal_intent
from play_internal.__main__ import main as workflow_main, validate_sealed, cache_snapshot
from workflow_lint import normalize
from play_internal.github import GitHub, verify_gates
from play_internal.play import ApiError, Publisher
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

    def test_prior_started_stage_is_negative_evidence_when_cache_was_lost(self):
        github=GitHub()
        previous={**run(True),'id':42,'run_attempt':2}
        jobs=[{'steps':[{'name':'Stage Internal bundle','status':'completed','conclusion':'failure'}]}]
        with patch.object(github,'pages',side_effect=[[previous],jobs]):
            self.assertTrue(github.prior_release_writes_possible(SOURCE,42,2))

    def test_skipped_publish_does_not_claim_a_version_was_uploaded(self):
        github=GitHub()
        previous={**run(True),'id':42,'run_attempt':2}
        jobs=[{'steps':[{'name':'Stage Internal bundle','status':'completed','conclusion':'skipped'}]}]
        with patch.object(github,'pages',side_effect=[[previous],jobs]):
            self.assertFalse(github.prior_release_writes_possible(SOURCE,42,2))

    def test_missing_checkpoint_after_possible_upload_stops_before_signing(self):
        with tempfile.TemporaryDirectory() as directory:
            env={'GITHUB_WORKSPACE':directory,'RUNNER_TEMP':directory,'PLAY_WORK_DIR':directory,
                 'GITHUB_RUN_ID':'42','GITHUB_RUN_ATTEMPT':'2'}
            with patch.dict(os.environ,env),patch.object(sys,'argv',['play-internal.py','restore']),\
                 patch('play_internal.__main__.source_env',return_value=SOURCE),\
                 patch('play_internal.__main__.GitHub') as client,patch('play_internal.artifacts.sign') as signer:
                client.return_value.prior_release_writes_possible.return_value=True
                with self.assertRaisesRegex(Stop,'never regenerate different bytes'):
                    workflow_main()
                signer.assert_not_called()

    def test_cache_save_warning_or_miss_cannot_allow_mutations(self):
        with tempfile.TemporaryDirectory() as directory:
            env={'GITHUB_WORKSPACE':directory,'RUNNER_TEMP':directory,'PLAY_WORK_DIR':directory,'CACHE_HIT':''}
            with patch.dict(os.environ,env),patch.object(sys,'argv',['play-internal.py','verify-signed-cache']),\
                 patch('play_internal.__main__.source_env',return_value=SOURCE),patch('play_internal.__main__.GitHub'):
                with self.assertRaisesRegex(Stop,'not independently restored'):
                    workflow_main()

    def test_backend_readback_cannot_reuse_residual_local_files(self):
        with tempfile.TemporaryDirectory() as directory:
            work=Path(directory)
            (work/'sealed').mkdir()
            (work/'sealed/bundle.enc').write_bytes(b'local-only-ciphertext')
            write_json(work/'sealed/provenance.json',{})
            write_json(work/'cache-expected-signed.json',cache_snapshot(work,'signed'))
            env={'GITHUB_WORKSPACE':directory,'RUNNER_TEMP':directory,'PLAY_WORK_DIR':directory,'CACHE_HIT':'true'}
            with patch.dict(os.environ,env),patch('play_internal.__main__.source_env',return_value=SOURCE),patch('play_internal.__main__.GitHub'):
                with patch.object(sys,'argv',['play-internal.py','park-signed-cache']):
                    workflow_main()
                self.assertFalse((work/'sealed').exists())
                with patch.object(sys,'argv',['play-internal.py','verify-signed-cache']),self.assertRaisesRegex(Stop,'did not restore every expected file'):
                    workflow_main()

    def test_commit_history_distinguishes_skipped_from_started_and_unknown(self):
        for step, expected in (({'status':'completed','conclusion':'skipped'},False),
                               ({'status':'queued','conclusion':None},False),
                               ({'status':'completed','conclusion':'failure'},True)):
            github=GitHub()
            jobs=[{'steps':[{'name':'Commit Internal and confirm serving version',**step}]}]
            with patch.object(github,'get',return_value=run(True)),patch.object(github,'pages',return_value=jobs):
                self.assertIs(github.commit_was_started(SOURCE,{'run':42,'attempt':1}),expected)
        with patch.object(GitHub,'get',return_value=run(True)),patch.object(GitHub,'pages',return_value=[]):
            self.assertIsNone(GitHub().commit_was_started(SOURCE,{'run':42,'attempt':1}))

    def test_only_one_official_cache_mode_is_validated_for_old_actionlint(self):
        text='jobs:\n  publish:\n    cache-mode: write\n    invalid-other-key: true\n'
        result=normalize('play-internal.yml',text)
        self.assertIn('invalid-other-key: true',result)  # Still rejected by actionlint.
        self.assertEqual(len(result.splitlines()),len(text.splitlines()))
        for filename,changed in (('ci.yml',text),('play-internal.yml',text.replace('publish:','verify:')),
                                 ('play-internal.yml',text.replace('write','invalid'))):
            with self.subTest(filename=filename,changed=changed),self.assertRaises(ValueError):
                normalize(filename,changed)

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

    def test_authenticated_cache_retains_original_unsigned_gates_and_exact_signed_bytes(self):
        root=self.root/'complete-checkpoint'
        unsigned,signed,sealed=root/'unsigned',root/'signed',root/'sealed'
        unsigned.mkdir(parents=True);signed.mkdir()
        shutil.copyfile(self.unsigned,unsigned/'bundle.aab')
        shutil.copyfile(self.signed,signed/'bundle.aab')
        manifest={**MANIFEST,'signedSha256':hashlib.sha256(self.signed.read_bytes()).hexdigest(),
                  'unsignedSha256':hashlib.sha256(self.unsigned.read_bytes()).hexdigest()}
        write_json(signed/'provenance.json',manifest)
        receipt=gates()[1]
        write_json(root/'gates.json',receipt)
        key=base64.b64encode(bytes(range(32))).decode()
        with patch.dict(os.environ,{'PLAY_CHECKPOINT_KEY':key}):
            seal(signed,sealed,unsigned,root/'gates.json')
            # Simulate deletion of run artifacts: only the encrypted cache remains.
            (signed/'bundle.aab').unlink()
            (unsigned/'bundle.aab').unlink()
            unseal(sealed,signed)
        self.assertEqual((signed/'bundle.aab').read_bytes(),self.signed.read_bytes())
        self.assertEqual((root/'original/unsigned/bundle.aab').read_bytes(),self.unsigned.read_bytes())
        self.assertEqual(json.loads((root/'original/gates/gates.json').read_text()),receipt)

    def test_public_cache_metadata_cannot_be_changed_outside_authenticated_container(self):
        root=self.root/'metadata-tamper'
        unsigned,signed,sealed=root/'unsigned',root/'signed',root/'sealed'
        unsigned.mkdir(parents=True);signed.mkdir()
        shutil.copyfile(self.unsigned,unsigned/'bundle.aab');shutil.copyfile(self.signed,signed/'bundle.aab')
        write_json(signed/'provenance.json',{**MANIFEST,'signedSha256':hashlib.sha256(self.signed.read_bytes()).hexdigest()})
        write_json(root/'gates.json',gates()[1])
        key=base64.b64encode(bytes(range(32))).decode()
        with patch.dict(os.environ,{'PLAY_CHECKPOINT_KEY':key}):
            seal(signed,sealed,unsigned,root/'gates.json')
            metadata=json.loads((sealed/'provenance.json').read_text())
            metadata['source']={**SOURCE,'attempt':99}
            write_json(sealed/'provenance.json',metadata)
            with self.assertRaisesRegex(Stop,'Unauthenticated checkpoint metadata'):
                unseal(sealed,signed)

    def test_intent_checkpoint_is_authenticated_and_roundtrips_edit_and_phase(self):
        root=self.root/'intent-checkpoint'
        intent={'editId':'edit-first-attempt','phase':'commit','signedSha256':'d'*64,'source':SOURCE}
        write_json(root/'intent/intent.json',intent)
        key=base64.b64encode(bytes(range(32))).decode()
        with patch.dict(os.environ,{'PLAY_CHECKPOINT_KEY':key}):
            seal_intent(root)
            (root/'intent/intent.json').unlink()
            unseal_intent(root)
            self.assertEqual(json.loads((root/'intent/intent.json').read_text()),intent)
            raw=bytearray((root/'intent-sealed/intent.enc').read_bytes());raw[-1]^=1
            (root/'intent-sealed/intent.enc').write_bytes(raw)
            with self.assertRaises(Stop):
                unseal_intent(root)


class FakePlay:
    """Separate deployed state from each staging edit; direct lifecycle API forbidden."""
    def __init__(self):
        self.calls = []
        self.bundles = [{"versionCode":477,"sha256":"e"*64}]
        self.deployed = {"internal":{"track":"internal","releases":[{"name":"old","versionCodes":["477"],"status":"completed"}]},
                         "production":{"track":"production","releases":[]},"beta":{"track":"beta","releases":[]}}
        self.edits = {}
        self.upload_error = self.put_error = self.commit_error = False
        self.upload_consumed = self.commit_accepted = self.commit_publishes = True
        self.expired = set()
        self.foreign_after_commit = False

    def request(self, method, path, body=None, upload=False):
        self.calls.append((method,path,copy.deepcopy(body) if not upload else "bundle"))
        if path == "/edits" and method == "POST":
            self.expired.update(self.edits)
            edit = f"edit{len(self.edits)+1}"
            self.edits[edit] = copy.deepcopy(self.deployed)
            return {"id":edit}
        assert path.startswith("/edits/"), "No direct cross-track lifecycle requests allowed"
        edit = path.split("/")[2].split(":")[0]
        if edit in self.expired:
            raise ApiError(404)
        if method == "GET":
            if path.endswith("/tracks"):
                return {"tracks":list(copy.deepcopy(self.edits[edit]).values())}
            if path.endswith("/tracks/internal"):
                return copy.deepcopy(self.edits[edit]["internal"])
            if path.endswith("/bundles"):
                return {"bundles":copy.deepcopy(self.bundles)}
            if path.endswith("/apks"):
                return {"apks":[]}
            return {"id":edit}
        if "/bundles?" in path:
            candidate = {"versionCode":479,"sha256":MANIFEST["signedSha256"]}
            if not self.upload_error or self.upload_consumed:
                self.bundles.append(candidate)
            if self.upload_error:
                raise ApiError(503)
            return candidate
        if method == "PUT":
            assert path.endswith("/tracks/internal")
            self.edits[edit]["internal"] = copy.deepcopy(body)
            if self.put_error:
                raise ApiError(503)
            return body
        if ":commit?" in path:
            if not self.commit_error or self.commit_accepted:
                if self.commit_publishes:
                    self.deployed = copy.deepcopy(self.edits[edit])
                self.expired.add(edit)
                if self.foreign_after_commit:
                    self.deployed["production"]["releases"] = [{"name":"new foreign draft","versionCodes":["478"],"status":"draft"}]
            if self.commit_error:
                raise ApiError(400 if not self.commit_accepted else 503)
            return {"id":edit}
        raise AssertionError((method,path))


class PublisherTests(unittest.TestCase):
    def setUp(self):
        self.api = FakePlay()
        self.fresh = unittest.mock.Mock()
        self.publisher = Publisher(self.api,copy.deepcopy(MANIFEST),self.fresh,sleep=lambda _:None)
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.intent_path = Path(self.temp.name)/"intent.json"

    def begin(self):
        self.assertEqual(self.publisher.begin(self.intent_path),(True,False))
        return read_intent(self.intent_path)

    def mutations(self, kind):
        return [call for call in self.api.calls if call[0]!="GET" and kind in call[1]]

    def deliver(self, intent):
        self.publisher.stage(intent,"fake.aab")
        intent["phase"] = "commit"
        write_json(self.intent_path,intent)
        return self.publisher.commit(intent)

    def test_success_is_fresh_deployed_internal_only_with_exact_hash(self):
        intent = self.begin()
        self.assertEqual(self.mutations("/bundles"),[])
        result = self.deliver(intent)
        self.assertEqual(result["outcome"],"SERVED")
        self.assertFalse(result["lifecycleEnumRead"])
        self.assertEqual(len(self.mutations("/bundles")),1)
        self.assertEqual([call[1] for call in self.mutations("/tracks")],["/edits/edit1/tracks/internal"])
        self.assertEqual(self.mutations(":commit")[0][1],"/edits/edit1:commit?changesInReviewBehavior=ERROR_IF_IN_REVIEW")
        self.assertGreaterEqual(len(self.api.edits),2)

    def test_staging_is_not_deployed_and_commit_requires_saved_phase(self):
        intent = self.begin()
        self.publisher.stage(intent,"fake.aab")
        self.assertEqual(self.api.deployed["internal"]["releases"][0]["versionCodes"],["477"])
        with self.assertRaisesRegex(Stop,"checkpoint was not persisted"):
            self.publisher.commit(intent)
        self.assertEqual(self.mutations(":commit"),[])

    def test_known_foreign_draft_or_unknown_status_stops_before_upload(self):
        for status in ("draft","statusUnspecified","unknown"):
            self.api.deployed["production"]["releases"] = [{"name":"foreign","versionCodes":["478"],"status":status}]
            with self.subTest(status=status),self.assertRaises(Stop):
                self.publisher.begin(self.intent_path)
        self.assertEqual(self.mutations("/bundles"),[])

    def test_in_review_is_rejected_atomically_without_cancel_or_repeated_commit(self):
        intent = self.begin()
        self.api.commit_error,self.api.commit_accepted = True,False
        with self.assertRaises(ApiError):
            self.deliver(intent)
        self.assertEqual(len(self.mutations(":commit")),1)
        self.assertNotIn("CANCEL",self.mutations(":commit")[0][1])

    def test_missing_internal_never_creates_or_falls_back_to_qa(self):
        del self.api.deployed["internal"]
        with self.assertRaises(Stop):
            self.publisher.begin(self.intent_path)
        self.assertEqual(self.mutations("/tracks"),[])

    def test_newer_or_different_occupied_version_cannot_be_uploaded_over(self):
        for code,digest in ((479,"foreign"),(480,"d"*64)):
            self.api.bundles = [{"versionCode":code,"sha256":digest}]
            with self.subTest(code=code),self.assertRaises(Stop):
                self.publisher.begin(self.intent_path)
        self.assertEqual(self.mutations("/bundles"),[])

    def test_restore_staged_active_edit_keeps_exact_bytes_and_does_not_reupload(self):
        intent = self.begin()
        self.publisher.stage(intent,"fake.aab")
        intent["phase"]="commit"
        write_json(self.intent_path,intent)
        self.assertEqual(self.publisher.begin(self.intent_path),(False,False))
        self.assertEqual(self.deliver(read_intent(self.intent_path))["outcome"],"SERVED")
        self.assertEqual(len(self.mutations("/bundles")),1)
        self.assertEqual(len(self.mutations("/tracks")),1)
        self.assertEqual(len(self.mutations(":commit")),1)

    def test_lost_upload_response_reads_existing_bundle_without_second_upload(self):
        intent = self.begin()
        self.api.upload_error=True
        self.assertEqual(self.deliver(intent)["outcome"],"SERVED")
        self.assertEqual(len(self.mutations("/bundles")),1)

    def test_unknown_upload_outcome_does_not_stage_or_commit(self):
        intent = self.begin()
        self.api.upload_error,self.api.upload_consumed=True,False
        with self.assertRaises(Stop):
            self.deliver(intent)
        self.assertEqual(self.mutations("/tracks"),[])
        self.assertEqual(self.mutations(":commit"),[])

    def test_lost_put_response_reads_same_edit_without_second_put(self):
        intent = self.begin()
        self.api.put_error=True
        self.deliver(intent)
        self.assertEqual(len(self.mutations("/tracks")),1)

    def test_lost_commit_response_confirms_deployed_without_second_commit(self):
        intent = self.begin()
        self.api.commit_error=True
        self.assertEqual(self.deliver(intent)["outcome"],"SERVED")
        self.assertEqual(len(self.mutations(":commit")),1)

    def test_commit_success_alone_is_not_delivery(self):
        intent = self.begin()
        self.api.commit_publishes=False
        with self.assertRaisesRegex(Stop,"availability not proven"):
            self.deliver(intent)

    def test_halted_or_wrong_hash_is_not_served_success(self):
        intent = self.begin()
        original=self.api.request
        def response(method,path,body=None,**kwargs):
            result=original(method,path,body,**kwargs)
            if ":commit?" in path:
                self.api.deployed["internal"]["releases"][0]["status"]="halted"
            return result
        with patch.object(self.api,"request",side_effect=response),self.assertRaises(Stop):
            self.deliver(intent)

    def test_foreign_draft_after_commit_does_not_negate_actual_delivery(self):
        intent=self.begin()
        self.api.foreign_after_commit=True
        self.assertEqual(self.deliver(intent)["outcome"],"SERVED")

    def test_expired_unused_edit_gets_new_edit_and_same_signed_digest(self):
        first=self.begin()
        self.api.expired.add(first["editId"])
        self.assertEqual(self.publisher.begin(self.intent_path),(True,False))
        renewed=read_intent(self.intent_path)
        self.assertNotEqual(first["editId"],renewed["editId"])
        self.assertEqual(first["signedSha256"],renewed["signedSha256"])
        self.assertEqual(self.mutations("/bundles"),[])

    def test_expired_owned_uploaded_bundle_reuses_without_upload(self):
        intent=self.begin()
        self.publisher.stage(intent,"fake.aab")
        self.api.expired.add(intent["editId"])
        self.assertEqual(self.publisher.begin(self.intent_path),(True,False))
        self.deliver(read_intent(self.intent_path))
        self.assertEqual(len(self.mutations("/bundles")),1)

    def test_committed_checkpoint_rerun_proves_serving_without_mutations(self):
        intent=self.begin()
        self.deliver(intent)
        before=len(self.mutations(":commit"))
        self.assertEqual(self.publisher.begin(self.intent_path),(False,True))
        self.assertEqual(len(self.mutations(":commit")),before)
        self.assertEqual(len(self.mutations("/bundles")),1)

    def test_uncertain_commit_marker_is_not_blindly_resubmitted(self):
        intent=self.begin()
        self.api.commit_publishes=False
        with self.assertRaises(Stop):
            self.deliver(intent)
        with self.assertRaises(Stop):
            self.publisher.begin(self.intent_path)
        self.assertEqual(len(self.mutations(":commit")),1)

    def test_expired_commit_marker_whose_post_never_started_can_resume(self):
        intent=self.begin()
        self.publisher.stage(intent,'fake.aab')
        intent['phase']='commit'
        write_json(self.intent_path,intent)
        self.api.expired.add(intent['editId'])
        self.publisher.commit_started=lambda intent:False
        self.assertEqual(self.publisher.begin(self.intent_path),(True,False))
        renewed=read_intent(self.intent_path)
        self.assertEqual(renewed['phase'],'prepared')
        self.assertEqual(self.deliver(renewed)['outcome'],'SERVED')
        self.assertEqual(len(self.mutations('/bundles')),1)
        self.assertEqual(len(self.mutations(':commit')),1)

    def test_tampered_intent_and_stale_source_stop_before_upload(self):
        intent=self.begin()
        for key,value in (("track","production"),("signedSha256","foreign"),("editId","../other"),("phase","unknown")):
            with self.subTest(key=key),self.assertRaises(Stop):
                self.publisher.stage({**intent,key:value},"fake.aab")
        self.fresh.side_effect=Stop("stale")
        with self.assertRaises(Stop):
            self.publisher.stage(intent,"fake.aab")
        self.assertEqual(self.mutations("/bundles"),[])


def read_intent(path):
    return json.loads(Path(path).read_text())


if __name__ == "__main__":
    unittest.main()

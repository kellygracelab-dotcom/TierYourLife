"""Nightly closed testing from the served Internal build. No network or Play writes."""
import copy
from pathlib import Path
import re
import sys
import unittest
from unittest.mock import patch

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from play_internal.alpha import Promoter, summary, wait_for_internal
from play_internal.github import GitHub
from play_internal.policy import CD_PATH, OWNER_ID, REPO_ID, Stop
from play_internal.transport import ApiError

DIGEST = "d" * 64
SOURCE = {"sha": "a" * 40, "runId": 1, "attempt": 1}
RECEIPT = {"outcome": "SERVED", "versionCode": 640, "signedSha256": DIGEST, "source": SOURCE}
PUBLISHED = "RELEASE_LIFECYCLE_STATE_PUBLISHED"
IN_REVIEW = "RELEASE_LIFECYCLE_STATE_IN_REVIEW"
WORKFLOWS = Path(__file__).resolve().parents[2] / "workflows"


class FakePlay:
    """Deployed tracks copied into each new edit; lifecycle as the releases API tells it."""

    def __init__(self):
        self.calls = []
        self.deployed = {
            "internal": {"track": "internal", "releases": [{"name": "internal-640-x", "versionCodes": ["640"], "status": "completed"}]},
            "alpha": {"track": "alpha", "releases": [{"name": "old", "versionCodes": ["600"], "status": "completed"}]},
            "production": {"track": "production", "releases": []},
        }
        self.bundles = [{"versionCode": 600, "sha256": "e" * 64}, {"versionCode": 640, "sha256": DIGEST}]
        self.states = {600: PUBLISHED}
        self.after_commit = PUBLISHED
        self.lifecycle_forbidden = False
        self.commit_error = None
        self.edits = {}

    def request(self, method, path, body=None, upload=False):
        assert not upload, "Closed testing never uploads"
        self.calls.append((method, path, copy.deepcopy(body)))
        if path == "/tracks/alpha/releases":
            if self.lifecycle_forbidden:
                raise ApiError(403)
            return {"releases": [{"track": "alpha", "activeArtifacts": [{"versionCode": str(code)}], "releaseLifecycleState": state}
                                 for code, state in self.states.items()]}
        if path == "/edits":
            edit = f"edit{len(self.edits) + 1}"
            self.edits[edit] = copy.deepcopy(self.deployed)
            return {"id": edit}
        edit = path.split("/")[2].split(":")[0]
        if method == "GET":
            if path.endswith("/tracks"):
                return {"tracks": list(copy.deepcopy(self.edits[edit]).values())}
            if path.endswith("/bundles"):
                return {"bundles": copy.deepcopy(self.bundles)}
            if "/tracks/" in path:
                return copy.deepcopy(self.edits[edit][path.rsplit("/", 1)[1]])
            return {"id": edit}
        if method == "PUT":
            assert path.endswith("/tracks/alpha"), "Only the alpha track is ever written"
            self.edits[edit]["alpha"] = copy.deepcopy(body)
            return copy.deepcopy(body)
        assert method == "POST" and path.endswith(":commit?changesInReviewBehavior=ERROR_IF_IN_REVIEW")
        if self.commit_error:
            raise ApiError(self.commit_error)
        self.deployed = copy.deepcopy(self.edits.pop(edit))
        self.states = {640: self.after_commit}
        return {"id": edit}

    def writes(self):
        return [call for call in self.calls if call[0] in ("PUT", "POST") and call[1] != "/edits"]


def promote(play, receipt=RECEIPT):
    return Promoter(play, lambda code: receipt, sleep=lambda seconds: None).promote()


class PromoteTests(unittest.TestCase):
    def test_the_served_internal_build_goes_to_alpha_and_is_confirmed_serving(self):
        play = FakePlay()
        result = promote(play)
        self.assertEqual("SERVED", result["outcome"])
        self.assertEqual(640, result["versionCode"])
        self.assertEqual(["completed"], [release["status"] for release in play.deployed["alpha"]["releases"]])
        self.assertEqual([{"name": "closed-640-aaaaaaaaaaaa", "versionCodes": ["640"], "status": "completed"}],
                         play.deployed["alpha"]["releases"])
        self.assertEqual(play.deployed["production"], {"track": "production", "releases": []})
        self.assertEqual(2, len(play.writes()))

    def test_already_in_alpha_writes_nothing(self):
        play = FakePlay()
        play.deployed["alpha"]["releases"][0]["versionCodes"] = ["640"]
        play.states = {640: PUBLISHED}
        result = promote(play)
        self.assertEqual("ALREADY_IN_ALPHA", result["outcome"])
        self.assertTrue(result["served"])
        self.assertEqual([], play.writes())

    def test_a_newer_alpha_is_left_alone(self):
        play = FakePlay()
        play.deployed["alpha"]["releases"][0]["versionCodes"] = ["641"]
        play.bundles.append({"versionCode": 641, "sha256": "f" * 64})
        self.assertEqual("ALREADY_IN_ALPHA", promote(play)["outcome"])
        self.assertEqual([], play.writes())

    def test_an_alpha_release_in_review_is_never_replaced(self):
        play = FakePlay()
        play.states = {600: IN_REVIEW}
        result = promote(play)
        self.assertEqual("SKIPPED_ALPHA_PENDING", result["outcome"])
        self.assertEqual({"600": IN_REVIEW}, result["alphaLifecycle"])
        self.assertIn(IN_REVIEW, summary(result))
        self.assertEqual([], play.writes())

    def test_a_rejected_alpha_release_stops_and_sends_nothing(self):
        play = FakePlay()
        play.states = {590: "RELEASE_LIFECYCLE_STATE_NOT_APPROVED", 600: PUBLISHED}
        result = promote(play)
        self.assertEqual("ALPHA_REJECTED", result["outcome"])
        self.assertEqual({"590": "RELEASE_LIFECYCLE_STATE_NOT_APPROVED", "600": PUBLISHED}, result["alphaLifecycle"])
        self.assertIn("RELEASE_LIFECYCLE_STATE_NOT_APPROVED", summary(result))
        self.assertEqual([], play.writes())

    def test_a_refused_read_before_is_asked_again(self):
        play = FakePlay()
        refusals = {"left": 2}
        real = play.request

        def flaky(method, path, body=None, upload=False):
            if path == "/tracks/alpha/releases" and refusals["left"]:
                refusals["left"] -= 1
                raise ApiError(404)
            return real(method, path, body, upload)
        play.request = flaky
        result = promote(play)
        self.assertEqual("SERVED", result["outcome"])
        self.assertEqual({"600": PUBLISHED}, result["alphaLifecycle"])

    def test_approved_but_unpublished_is_left_for_the_console(self):
        play = FakePlay()
        play.states = {600: "RELEASE_LIFECYCLE_STATE_APPROVED_NOT_PUBLISHED"}
        self.assertEqual("SKIPPED_ALPHA_PENDING", promote(play)["outcome"])
        self.assertEqual([], play.writes())

    def test_a_refused_read_after_commit_does_not_end_the_wait(self):
        play = FakePlay()
        play.after_commit = IN_REVIEW
        calls = {"n": 0}
        real = play.request

        def flaky(method, path, body=None, upload=False):
            if path == "/tracks/alpha/releases" and play.states.get(640):
                calls["n"] += 1
                if calls["n"] == 2:
                    raise ApiError(404)
            return real(method, path, body, upload)
        play.request = flaky
        result = promote(play)
        self.assertEqual("SENT_FOR_REVIEW", result["outcome"])
        self.assertEqual(IN_REVIEW, result["lifecycle"])

    def test_a_state_this_code_does_not_know_holds_closed_testing(self):
        play = FakePlay()
        play.states = {600: "RELEASE_LIFECYCLE_STATE_SOMETHING_NEW"}
        result = promote(play)
        self.assertEqual("SKIPPED_ALPHA_PENDING", result["outcome"])
        self.assertIn("RELEASE_LIFECYCLE_STATE_SOMETHING_NEW", summary(result))
        self.assertEqual([], play.writes())

    def test_our_version_sent_and_not_live_is_never_sent_again(self):
        for state in ("RELEASE_LIFECYCLE_STATE_NOT_SENT_FOR_REVIEW", "RELEASE_LIFECYCLE_STATE_UNSPECIFIED", IN_REVIEW):
            play = FakePlay()
            play.states = {600: PUBLISHED, 640: state}
            result = promote(play)
            with self.subTest(state=state):
                self.assertEqual("ALREADY_SENT", result["outcome"])
                self.assertEqual(state, result["lifecycle"])
                self.assertEqual([], play.writes())

    def test_committed_in_a_state_not_known_says_its_name(self):
        play = FakePlay()
        play.after_commit = "RELEASE_LIFECYCLE_STATE_NOT_SENT_FOR_REVIEW"
        result = promote(play)
        self.assertEqual("COMMITTED", result["outcome"])
        self.assertIn("RELEASE_LIFECYCLE_STATE_NOT_SENT_FOR_REVIEW", summary(result))

    def test_a_draft_anywhere_is_preserved(self):
        play = FakePlay()
        play.deployed["production"]["releases"] = [{"name": "p", "versionCodes": ["600"], "status": "draft"}]
        self.assertEqual("SKIPPED_DRAFT_IN_CONSOLE", promote(play)["outcome"])
        self.assertEqual([], play.writes())

    def test_an_unsettled_alpha_rollout_is_left_alone(self):
        play = FakePlay()
        play.deployed["alpha"]["releases"][0]["status"] = "halted"
        self.assertEqual("SKIPPED_ALPHA_NOT_SETTLED", promote(play)["outcome"])
        self.assertEqual([], play.writes())

    def test_a_commit_the_server_refuses_changes_nothing_and_waits_for_next_night(self):
        play = FakePlay()
        play.commit_error = 400
        result = promote(play)
        self.assertEqual("NOT_SENT", result["outcome"])
        self.assertEqual("old", play.deployed["alpha"]["releases"][0]["name"])

    def test_a_forbidden_commit_fails_loudly(self):
        play = FakePlay()
        play.commit_error = 403
        with self.assertRaises(Stop):
            promote(play)

    def test_still_in_review_after_commit_says_so(self):
        play = FakePlay()
        play.after_commit = IN_REVIEW
        result = promote(play)
        self.assertEqual("SENT_FOR_REVIEW", result["outcome"])
        self.assertFalse(result["served"])

    def test_a_rejected_release_fails_loudly(self):
        play = FakePlay()
        play.after_commit = "RELEASE_LIFECYCLE_STATE_NOT_APPROVED"
        with self.assertRaises(Stop):
            promote(play)

    def test_unreadable_lifecycle_sends_nothing(self):
        play = FakePlay()
        play.lifecycle_forbidden = True
        result = promote(play)
        self.assertEqual("SKIPPED_LIFECYCLE_UNREADABLE", result["outcome"])
        self.assertIsNone(result["alphaLifecycle"])
        self.assertIn("not readable", summary(result))
        self.assertEqual([], play.writes())

    def test_unreadable_only_after_commit_never_claims_review_state(self):
        play = FakePlay()
        real = play.request

        def after(method, path, body=None, upload=False):
            if path == "/tracks/alpha/releases" and 640 in play.states:
                raise ApiError(403)
            return real(method, path, body, upload)
        play.request = after
        result = promote(play)
        self.assertEqual("COMMITTED", result["outcome"])
        self.assertIsNone(result["lifecycle"])
        self.assertEqual({"600": PUBLISHED}, result["alphaLifecycle"])

    def test_bytes_other_than_the_served_receipt_are_never_promoted(self):
        for receipt in ({**RECEIPT, "signedSha256": "0" * 64}, {**RECEIPT, "outcome": "STAGED"}, {**RECEIPT, "versionCode": 639}):
            play = FakePlay()
            with self.subTest(receipt=receipt), self.assertRaises(Stop):
                promote(play, receipt)
            self.assertEqual([], play.writes())

    def test_no_closed_testing_track_writes_nothing(self):
        play = FakePlay()
        del play.deployed["alpha"]
        with self.assertRaises(Stop):
            promote(play)
        self.assertEqual([], play.writes())

    def test_internal_without_a_single_completed_release_writes_nothing(self):
        play = FakePlay()
        play.deployed["internal"]["releases"][0]["status"] = "inProgress"
        with self.assertRaises(Stop):
            promote(play)
        self.assertEqual([], play.writes())


class ReceiptTrustTests(unittest.TestCase):
    def test_a_receipt_from_anything_but_main_play_internal_is_refused(self):
        repo = {"id": REPO_ID, "owner": {"id": OWNER_ID}}
        good = {"id": 5, "path": CD_PATH, "event": "workflow_run", "head_branch": "main",
                "repository": repo, "head_repository": copy.deepcopy(repo)}
        for key, value in (("path", ".github/workflows/ci.yml"), ("event", "pull_request"), ("head_branch", "topic")):
            run = {**good, key: value}
            github = GitHub()
            with self.subTest(key=key), patch.object(github, "get", return_value={"workflow_runs": [run]}), self.assertRaises(Stop):
                github.served_receipt(640, "unused")


class WaitTests(unittest.TestCase):
    def test_waits_for_another_internal_run_then_goes(self):
        listings = iter([[{"id": 7, "status": "in_progress"}, {"id": 9, "status": "in_progress"}], [{"id": 9, "status": "in_progress"}]])
        slept = []
        wait_for_internal(lambda: next(listings), 9, sleep=slept.append)
        self.assertEqual([30], slept)

    def test_gives_up_for_the_night_when_internal_never_finishes(self):
        with self.assertRaises(Stop):
            wait_for_internal(lambda: [{"id": 7, "status": "queued"}], 9, sleep=lambda seconds: None, rounds=3)


class WorkflowTests(unittest.TestCase):
    def jobs(self):
        text = (WORKFLOWS / "play-internal.yml").read_text(encoding="utf-8")
        parts = re.split(r"(?m)^  ([a-z][a-z0-9-]*):\n", text.split("\njobs:\n", 1)[1])
        return text, dict(zip(parts[1::2], parts[2::2]))

    def test_the_alpha_job_reads_no_signing_secret_and_only_answers_the_nightly_clock(self):
        text, jobs = self.jobs()
        self.assertIn("workflows: [CI, Play Alpha Night]", text)
        alpha = jobs["alpha"]
        self.assertNotIn("secrets.", alpha)
        self.assertIn("github.event.workflow_run.path == '.github/workflows/play-alpha-night.yml'", alpha)
        self.assertIn("github.event.workflow_run.head_branch == 'main'", alpha)
        self.assertNotIn("production", alpha)

    def test_internal_still_answers_only_ci(self):
        _, jobs = self.jobs()
        self.assertIn("github.event.workflow_run.path == '.github/workflows/ci.yml'", jobs["verify"])
        self.assertIn("needs: verify", jobs["publish"])

    def test_the_clock_has_no_permissions(self):
        night = (WORKFLOWS / "play-alpha-night.yml").read_text(encoding="utf-8")
        self.assertIn("permissions: {}", night)
        self.assertNotIn("secrets.", night)
        self.assertNotIn("uses:", night)


if __name__ == "__main__":
    unittest.main()

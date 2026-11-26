"""Nightly closed testing: the newest Internal build already served goes to alpha.

Nothing is built, signed or uploaded: the same version code and AAB already in
Play. Only the alpha track is written, with ERROR_IF_IN_REVIEW; production and
other tracks are never touched, and a review is never cancelled.
"""
import re
import time

from .play import version
from .policy import PACKAGE, TRACK, Stop, require
from .transport import ApiError

ALPHA = "alpha"
PUBLISHED = "RELEASE_LIFECYCLE_STATE_PUBLISHED"
# Committed and on its way: Google's review, or approved and waiting for a manual publish.
WAITING = {"RELEASE_LIFECYCLE_STATE_IN_REVIEW": "SENT_FOR_REVIEW",
           "RELEASE_LIFECYCLE_STATE_APPROVED_NOT_PUBLISHED": "APPROVED_NOT_PUBLISHED"}
REJECTED = "RELEASE_LIFECYCLE_STATE_NOT_APPROVED"
# Only these let a new release in: live, or rejected and so replaceable. Anything
# else (in review, waiting to be sent, a state this code does not know) holds it.
SETTLED = (PUBLISHED, REJECTED)
KNOWN_STATUSES = ("completed", "inProgress", "halted", "draft")


def release_codes(release):
    items = release.get("versionCodes", [])
    require(isinstance(items, list), "Invalid track version codes")
    return [version(code) for code in items]


class Promoter:
    def __init__(self, api, served_receipt, *, sleep=time.sleep):
        self.api, self.served_receipt, self.sleep = api, served_receipt, sleep

    def new_edit(self):
        edit = self.api.request("POST", "/edits", {})
        require(re.fullmatch(r"[A-Za-z0-9_-]+", edit.get("id", "")) is not None, "Unexpected edit id")
        return edit["id"]

    def tracks(self, edit):
        tracks = self.api.request("GET", f"/edits/{edit}/tracks").get("tracks")
        require(isinstance(tracks, list) and all(isinstance(item, dict) for item in tracks), "Invalid track snapshot")
        named = {item.get("track"): item for item in tracks}
        require(len(named) == len(tracks) and all(isinstance(name, str) and name for name in named), "Ambiguous track snapshot")
        for track in tracks:
            releases = track.get("releases", [])
            require(isinstance(releases, list) and all(isinstance(item, dict) for item in releases), "Invalid track releases")
            for release in releases:
                require(release.get("status") in KNOWN_STATUSES, "Unknown track status; nothing written")
                release_codes(release)
        return named

    def bundle_sha(self, edit, code):
        bundles = self.api.request("GET", f"/edits/{edit}/bundles").get("bundles", [])
        require(isinstance(bundles, list) and all(isinstance(item, dict) for item in bundles), "Invalid bundle list")
        matches = [item for item in bundles if version(item.get("versionCode")) == code]
        require(len(matches) == 1 and isinstance(matches[0].get("sha256"), str), "Internal version has no single uploaded bundle")
        return matches[0]["sha256"].lower()

    def lifecycle(self):
        """Alpha's release states by version code, or None where this account may not read them."""
        try:
            data = self.api.request("GET", f"/tracks/{ALPHA}/releases")
        except ApiError as error:
            require(error.status in (403, 404), "Cannot read closed testing releases")
            return None
        require(set(data) <= {"releases"}, "Unexpected release-list schema")
        releases = data.get("releases", [])
        require(isinstance(releases, list) and len(releases) < 20, "Invalid or capped release list")
        states = {}
        for release in releases:
            require(isinstance(release, dict) and release.get("track") == ALPHA, "Release track metadata mismatch")
            artifacts = release.get("activeArtifacts", [])
            require(isinstance(artifacts, list) and all(isinstance(item, dict) for item in artifacts), "Invalid release artifacts")
            for item in artifacts:
                states[version(item.get("versionCode"))] = release.get("releaseLifecycleState")
        return states

    def alpha_serves(self, code, name, digest):
        edit = self.new_edit()
        track = self.api.request("GET", f"/edits/{edit}/tracks/{ALPHA}")
        releases = track.get("releases", [])
        shown = (track.get("track") == ALPHA and isinstance(releases, list) and len(releases) == 1
                 and releases[0].get("versionCodes") == [str(code)] and releases[0].get("status") == "completed"
                 and (name is None or releases[0].get("name") == name))
        return shown and self.bundle_sha(edit, code) == digest

    def promote(self):
        edit = self.new_edit()
        tracks = self.tracks(edit)
        require(ALPHA in tracks, "Closed testing track 'alpha' not found; nothing written")
        if any(release.get("status") == "draft" for track in tracks.values() for release in track.get("releases", [])):
            return {"outcome": "SKIPPED_DRAFT_IN_CONSOLE"}
        internal = tracks.get(TRACK, {}).get("releases", [])
        require(len(internal) == 1 and internal[0].get("status") == "completed" and len(release_codes(internal[0])) == 1,
                "Internal has no single completed release; nothing to promote")
        code = release_codes(internal[0])[0]
        digest = self.bundle_sha(edit, code)
        receipt = self.served_receipt(code)
        require(receipt.get("outcome") == "SERVED" and receipt.get("versionCode") == code
                and receipt.get("signedSha256") == digest, "Internal build lacks this workflow's SERVED receipt for the same bytes")
        base = {"versionCode": code, "signedSha256": digest, "internalSource": receipt.get("source")}
        alpha = tracks[ALPHA].get("releases", [])
        states = self.lifecycle()
        base["alphaLifecycle"] = None if states is None else {str(number): state for number, state in sorted(states.items())}
        in_alpha = [number for release in alpha for number in release_codes(release)]
        if in_alpha and max(in_alpha) >= code:
            latest = max(in_alpha)
            state = None if states is None else states.get(latest)
            served = state == PUBLISHED and self.alpha_serves(latest, None, self.bundle_sha(edit, latest))
            return {**base, "outcome": "ALREADY_IN_ALPHA", "alphaVersionCode": latest, "lifecycle": state, "served": served}
        if states and code in states:
            # Sent already and not yet live: never send it again.
            return {**base, "outcome": "ALREADY_SENT", "lifecycle": states[code]}
        if any(release.get("status") != "completed" for release in alpha):
            return {**base, "outcome": "SKIPPED_ALPHA_NOT_SETTLED"}
        if states and any(state not in SETTLED for state in states.values()):
            return {**base, "outcome": "SKIPPED_ALPHA_PENDING"}
        name = f"closed-{code}-{str(receipt['source']['sha'])[:12]}"
        target = {"track": ALPHA, "releases": [{"name": name, "versionCodes": [str(code)], "status": "completed"}]}
        try:
            staged = self.api.request("PUT", f"/edits/{edit}/tracks/{ALPHA}", target)
        except ApiError:
            staged = self.api.request("GET", f"/edits/{edit}/tracks/{ALPHA}")
        require(staged.get("track") == ALPHA and staged.get("releases") == target["releases"], "Closed testing staging not confirmed")
        try:
            self.api.request("POST", f"/edits/{edit}:commit?changesInReviewBehavior=ERROR_IF_IN_REVIEW")
        except ApiError as failure:
            try:
                self.api.request("GET", f"/edits/{edit}")
            except ApiError as state:
                # The edit is gone: the commit may have gone through. Read what Play shows.
                require(state.status in (400, 404, 410), "Commit outcome unknown")
            else:
                # Still open: nothing was committed. Something in review, most likely: next night.
                require(failure.status in (400, 409), str(failure))
                return {**base, "outcome": "NOT_SENT", "reason": str(failure)}
        return {**base, **self.after_commit(code, name, digest), "releaseName": name}

    def after_commit(self, code, name, digest):
        state, readable = None, False
        for attempt in range(24):
            states = self.lifecycle()
            # Enum names only: what the release went through, for the run's log.
            print(f"Alpha lifecycle after commit: {states}")
            if states is None:
                # Once read, a later refusal is a hiccup, not an answer: keep asking.
                if not readable:
                    return {"outcome": "COMMITTED", "lifecycle": None, "served": self.alpha_serves(code, name, digest)}
                if attempt < 23:
                    self.sleep(5)
                continue
            readable = True
            state = states.get(code)
            require(state != REJECTED, f"Closed testing release {code} was rejected: {REJECTED}")
            if state == PUBLISHED and self.alpha_serves(code, name, digest):
                return {"outcome": "SERVED", "lifecycle": state, "served": True}
            if attempt < 23:
                self.sleep(5)
        require(state is not None, "Committed release does not show in closed testing")
        # Committed: anything else is said by its own name, not guessed.
        return {"outcome": WAITING.get(state, "COMMITTED"), "lifecycle": state, "served": False}


SAID = {
    "SERVED": "выдаётся тестерам",
    "SENT_FOR_REVIEW": "отправлено на проверку Google",
    "APPROVED_NOT_PUBLISHED": "проверено, ждёт ручной публикации в Консоли",
    "COMMITTED": "отправлено; выдачу тестерам Play пока не подтвердил",
    "ALREADY_IN_ALPHA": "в закрытом тесте уже эта версия или новее",
    "ALREADY_SENT": "эта версия уже отправлена в закрытый тест и ещё не выдаётся; не трогаем",
    "NOT_SENT": "не отправлено, ничего не изменено: у приложения есть изменения на проверке или Play отказал; повтор следующей ночью",
    "SKIPPED_DRAFT_IN_CONSOLE": "пропуск: в Консоли есть черновик",
    "SKIPPED_ALPHA_NOT_SETTLED": "пропуск: выпуск закрытого теста не завершён",
    "SKIPPED_ALPHA_PENDING": "пропуск: выпуск закрытого теста ещё не выдаётся (на проверке, ждёт отправки или в незнакомом состоянии)",
}


def summary(result):
    line = f"Closed testing ({ALPHA}) {PACKAGE}: **{result['outcome']}** — {SAID[result['outcome']]}."
    if "versionCode" in result:
        line += f" Internal served {result['versionCode']}, AAB SHA-256 `{result['signedSha256']}`."
    if result.get("lifecycle"):
        line += f" State: {result['lifecycle']}."
    if "alphaLifecycle" in result:
        line += f" Alpha lifecycle before: {result['alphaLifecycle'] or 'not readable by this account'}."
    return line + "\n"


def other_runs_going(runs, current):
    """Other Play Internal runs still going; an alpha commit would invalidate their open edit."""
    return [run for run in runs if int(run["id"]) != current and run.get("status") != "completed"]


def wait_for_internal(list_runs, current, *, sleep=time.sleep, rounds=60):
    for attempt in range(rounds):
        if not other_runs_going(list_runs(), current):
            return
        if attempt < rounds - 1:
            sleep(30)
    raise Stop("Internal publishing still running; closed testing waits for the next night")

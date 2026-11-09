"""Conservative Play edit state machine. All writes have one fixed track."""
from pathlib import Path
import re
import time
import urllib.parse

from .policy import PACKAGE, TRACK, Stop, read_json, require, write_json
from .transport import ApiError, PlayApi

PUBLISHED = "RELEASE_LIFECYCLE_STATE_PUBLISHED"


def version(value):
    require(type(value) in (str, int) and re.fullmatch(r"[1-9][0-9]*", str(value)) is not None, "Unexpected Play version code")
    return int(value)


class Publisher:
    def __init__(self, api, manifest, fresh, *, sleep=time.sleep):
        self.api, self.manifest, self.fresh, self.sleep = api, manifest, fresh, sleep
        self.code = manifest["versionCode"]
        self.digest = manifest["signedSha256"]
        self.name = f"internal-{self.code}-{manifest['source']['sha'][:12]}"

    def releases(self, track):
        require(isinstance(track, str) and 0 < len(track) <= 128 and not re.search(r"[/\x00-\x1f\x7f]", track), "Invalid Play track id")
        data = self.api.request("GET", f"/tracks/{urllib.parse.quote(track, safe='')}/releases")
        # Discovery ListReleaseSummariesResponse has only `releases`, no page
        # token/page-size parameters. {} is a valid protobuf-JSON empty list;
        # empty bytes/HTML/null/arbitrary objects never reach this point.
        require(isinstance(data, dict) and set(data) <= {"releases"}, "Unexpected release-list schema or pagination; cannot prove queue state")
        releases = data.get("releases", [])
        require(isinstance(releases, list) and len(releases) < 20, "Invalid or capped release list; cannot prove complete queue state")
        for release in releases:
            require(isinstance(release, dict) and release.get("track") == track, "Release track metadata mismatch")
            artifacts = release.get("activeArtifacts", [])
            require(isinstance(artifacts, list) and all(isinstance(item, dict) and "versionCode" in item for item in artifacts), "Invalid release artifact schema")
            for item in artifacts:
                version(item["versionCode"])
        return releases

    def target_lifecycle(self):
        for release in self.releases(TRACK):
            codes = [version(artifact["versionCode"]) for artifact in release.get("activeArtifacts", [])]
            if self.code in codes:
                require(release.get("track") == TRACK and release.get("releaseName") == self.name and codes == [self.code], "Target version belongs to another release")
                lifecycle = release.get("releaseLifecycleState")
                require(lifecycle in (PUBLISHED, "RELEASE_LIFECYCLE_STATE_DRAFT", "RELEASE_LIFECYCLE_STATE_NOT_SENT_FOR_REVIEW",
                                      "RELEASE_LIFECYCLE_STATE_IN_REVIEW", "RELEASE_LIFECYCLE_STATE_APPROVED_NOT_PUBLISHED"),
                        "Own release rejected or has unknown lifecycle; Console action needed")
                return lifecycle
        return None

    def published(self):
        return self.target_lifecycle() == PUBLISHED

    def new_edit(self):
        self.fresh()
        edit = self.api.request("POST", "/edits", {})
        require(re.fullmatch(r"[A-Za-z0-9_-]+", edit.get("id", "")) is not None, "Unexpected edit id")
        return edit["id"]

    def snapshot(self, edit, *, own_intent=False):
        prefix = f"/edits/{edit}"
        tracks = self.api.request("GET", prefix + "/tracks").get("tracks", [])
        names = [track.get("track") for track in tracks]
        require(TRACK in names and len(names) == len(set(names)), "Real internal track missing or ambiguous; no track fallback")
        internal = self.api.request("GET", prefix + "/tracks/internal")
        require(internal.get("track") == TRACK, "Unexpected Internal track response")
        codes = []
        for track in tracks:
            require(isinstance(track.get("track"), str) and bool(track["track"]), "Invalid track id")
            for release in track.get("releases", []):
                codes.extend(version(code) for code in release.get("versionCodes", []))
            # App-read metadata is required. A 403 stops; do not request broader
            # production release grants or silently omit this check.
            for release in self.releases(track["track"]):
                require(release.get("track") == track["track"], "Release track metadata mismatch")
                own = (own_intent and track["track"] == TRACK and release.get("releaseName") == self.name
                       and [version(item["versionCode"]) for item in release.get("activeArtifacts", [])] == [self.code]
                       and release.get("releaseLifecycleState") in ("RELEASE_LIFECYCLE_STATE_DRAFT", "RELEASE_LIFECYCLE_STATE_NOT_SENT_FOR_REVIEW",
                                                                 "RELEASE_LIFECYCLE_STATE_IN_REVIEW", "RELEASE_LIFECYCLE_STATE_APPROVED_NOT_PUBLISHED"))
                require(own or release.get("releaseLifecycleState") == PUBLISHED, "Pending/rejected/unknown Play changes exist; preserve the manual Console queue")
                codes.extend(version(item["versionCode"]) for item in release.get("activeArtifacts", []))
        bundles = self.api.request("GET", prefix + "/bundles").get("bundles", [])
        apks = self.api.request("GET", prefix + "/apks").get("apks", [])
        codes.extend(version(item["versionCode"]) for item in bundles + apks)
        require(not codes or max(codes) <= self.code, "A newer Play version exists; refusing a downgrade")
        candidates = [item for item in bundles if version(item["versionCode"]) == self.code]
        require(len(candidates) <= 1, "Ambiguous uploaded bundle version")
        if candidates:
            require(candidates[0].get("sha256", "").lower() == self.digest, "Version code already occupied by different AAB bytes")
        elif self.code in codes:
            raise Stop("Version code occupied without a matching bundle; never blindly re-upload")
        return bool(candidates)

    def validate_intent(self, intent):
        require(intent.get("schema") == 1 and intent.get("source") == self.manifest["source"], "Wrong durable edit intent source")
        require(intent.get("signedSha256") == self.digest and intent.get("versionCode") == self.code and intent.get("track") == TRACK and intent.get("releaseName") == self.name, "Edit intent differs from sealed signed artifact")
        require(re.fullmatch(r"[A-Za-z0-9_-]+", intent.get("editId", "")) is not None, "Invalid durable edit id")

    def begin(self, path):
        path = Path(path)
        self.fresh()
        if path.exists():
            intent = read_json(path)
            self.validate_intent(intent)
            if self.target_lifecycle() is not None:
                self.wait_published()
                self.confirm_serving()
                return False, True
            try:
                self.api.request("GET", f"/edits/{intent['editId']}")
            except ApiError as error:
                require(error.status in (400, 404, 410), "Cannot read saved edit; retry after external issue is resolved")
                inspection = self.new_edit()
                self.snapshot(inspection, own_intent=True)
                # The queue is clear and the code is free or holds our exact
                # bytes. A NEW intent can stage owned bytes without re-upload.
                self.write_intent(path, inspection)
                return True, False
            self.snapshot(intent["editId"], own_intent=True)
            return False, False
        edit = self.new_edit()
        occupied = self.snapshot(edit)
        if occupied:
            require(self.published(), "Occupied target has no durable edit receipt; manual reconciliation required")
            self.confirm_serving()
            return False, True
        self.write_intent(path, edit)
        # Workflow must upload this intent successfully BEFORE publish() starts.
        return True, False

    def write_intent(self, path, edit):
        write_json(path, {"schema": 1, "source": self.manifest["source"], "signedSha256": self.digest,
                          "versionCode": self.code, "track": TRACK, "releaseName": self.name, "editId": edit})

    def wait_published(self):
        for attempt in range(24):
            if self.published():
                return
            if attempt < 23:
                self.sleep(5)
        raise Stop("Edit committed but availability not proven by releases.list; do not report published")

    def receipt(self, *, already=False):
        return {"outcome": "PUBLISHED", "alreadyPublished": already, "package": PACKAGE, "track": TRACK, "status": "completed",
                "versionCode": self.code, "releaseName": self.name, "signedSha256": self.digest, "source": self.manifest["source"]}

    def confirm_serving(self):
        # PUBLISHED also includes halted releases. Independently confirm the
        # currently serving Internal target is completed, not a halted fallback.
        # Create the read snapshot AFTER waiting for publication. Foreign queues
        # matter before release writes, not when proving a completed delivery.
        edit = self.new_edit()
        track = self.api.request("GET", f"/edits/{edit}/tracks/internal")
        require(self.track_matches(track), "Published candidate is not the current completed Internal release")
        bundles = self.api.request("GET", f"/edits/{edit}/bundles").get("bundles", [])
        require(isinstance(bundles, list), "Invalid serving bundle list")
        candidates = [item for item in bundles if isinstance(item, dict) and version(item.get("versionCode")) == self.code]
        require(len(candidates) == 1 and isinstance(candidates[0].get("sha256"), str)
                and candidates[0]["sha256"].lower() == self.digest, "Published bundle digest cannot be confirmed")

    def track_matches(self, track):
        releases = track.get("releases", [])
        return (track.get("track") == TRACK and len(releases) == 1
                and releases[0].get("name") == self.name and releases[0].get("versionCodes") == [str(self.code)]
                and releases[0].get("status") == "completed")

    def publish(self, intent, bundle):
        self.validate_intent(intent)
        edit = intent["editId"]
        prefix = f"/edits/{edit}"
        self.fresh()
        occupied = self.snapshot(edit, own_intent=True)
        if not occupied:
            self.fresh()
            try:
                uploaded = self.api.request("POST", prefix + "/bundles?uploadType=media", bundle, upload=True)
                require(version(uploaded.get("versionCode")) == self.code and uploaded.get("sha256", "").lower() == self.digest, "Play upload response differs from sealed AAB")
            except ApiError:
                # A response can be lost after Play has consumed the version.
                require(self.snapshot(edit, own_intent=True), "Upload outcome uncertain; saved edit retained, no blind re-upload")
        self.fresh()
        # Recheck Console pending changes and monotonic versions before staging.
        self.snapshot(edit, own_intent=True)
        target = {"track": TRACK, "releases": [{"name": self.name, "versionCodes": [str(self.code)], "status": "completed"}]}
        current = self.api.request("GET", prefix + "/tracks/internal")
        if not self.track_matches(current):
            self.fresh()
            try:
                staged = self.api.request("PUT", prefix + "/tracks/internal", target)
            except ApiError:
                staged = self.api.request("GET", prefix + "/tracks/internal")
            require(self.track_matches(staged), "Internal staging not confirmed; saved edit retained")
        # An edit may expose the candidate through the direct releases API as a
        # draft. Check other tracks again; candidate draft is ours, not foreign.
        for track in self.api.request("GET", prefix + "/tracks").get("tracks", []):
            for release in self.releases(track["track"]):
                own = (track["track"] == TRACK and release.get("releaseName") == self.name
                       and [version(item["versionCode"]) for item in release.get("activeArtifacts", [])] == [self.code])
                require(own or release.get("releaseLifecycleState") == PUBLISHED, "Foreign pending changes detected before commit")
        self.fresh()
        try:
            self.api.request("POST", prefix + ":commit?changesInReviewBehavior=ERROR_IF_IN_REVIEW")
        except ApiError as error:
            # Never repeat commit or cancel somebody else's review.
            try:
                self.wait_published()
            except Stop:
                raise error from None
        self.wait_published()
        self.confirm_serving()
        return self.receipt()

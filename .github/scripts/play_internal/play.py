"""Conservative Play edit state machine. All writes have one fixed track."""
import json
from pathlib import Path
import re
import time
import urllib.error
import urllib.parse
import urllib.request

from .policy import PACKAGE, TRACK, Stop, read_json, require, write_json

BASE = f"https://androidpublisher.googleapis.com/androidpublisher/v3/applications/{PACKAGE}"
UPLOAD = f"https://androidpublisher.googleapis.com/upload/androidpublisher/v3/applications/{PACKAGE}"
PUBLISHED = "RELEASE_LIFECYCLE_STATE_PUBLISHED"


class ApiError(Stop):
    def __init__(self, status):
        self.status = status
        super().__init__(f"Play API request failed (HTTP {status}); no automatic permission/review fallback")


class PlayApi:
    def __init__(self, token):
        require(bool(token), "Missing short-lived Play access token")
        self.token = token

    def request(self, method, path, body=None, *, upload=False):
        data = Path(body).read_bytes() if upload else (json.dumps(body).encode() if body is not None else None)
        headers = {"Authorization": f"Bearer {self.token}", "Content-Type": "application/octet-stream" if upload else "application/json"}
        url = (UPLOAD if upload else BASE) + path
        # Only GETs have transport retries. POST upload/commit can have succeeded
        # despite a lost response, so their caller must reconcile state first.
        attempts = 3 if method == "GET" else 1
        for attempt in range(attempts):
            try:
                request = urllib.request.Request(url, data=data, headers=headers, method=method)
                with urllib.request.urlopen(request, timeout=180) as response:
                    return json.load(response)
            except urllib.error.HTTPError as error:
                status = error.code
                error.close()
                if status not in (429, 500, 502, 503, 504) or attempt == attempts - 1:
                    raise ApiError(status) from None
            except (urllib.error.URLError, TimeoutError):
                if attempt == attempts - 1:
                    raise ApiError(0) from None
            time.sleep(2 ** attempt)


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
        return self.api.request("GET", f"/tracks/{urllib.parse.quote(track, safe='')}/releases").get("releases", [])

    def published(self):
        for release in self.releases(TRACK):
            codes = [version(artifact["versionCode"]) for artifact in release.get("activeArtifacts", [])]
            if self.code in codes:
                require(release.get("track") == TRACK and release.get("releaseName") == self.name and codes == [self.code], "Target version belongs to another release")
                lifecycle = release.get("releaseLifecycleState")
                if lifecycle == PUBLISHED:
                    return True
                require(lifecycle in ("RELEASE_LIFECYCLE_STATE_DRAFT", "RELEASE_LIFECYCLE_STATE_NOT_SENT_FOR_REVIEW",
                                      "RELEASE_LIFECYCLE_STATE_IN_REVIEW", "RELEASE_LIFECYCLE_STATE_APPROVED_NOT_PUBLISHED"),
                        "Own release rejected or has unknown lifecycle; Console action needed")
                return False
        return False

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
            try:
                self.api.request("GET", f"/edits/{intent['editId']}")
            except ApiError as error:
                require(error.status in (400, 404, 410), "Cannot read saved edit; retry after external issue is resolved")
                published = self.published()
                inspection = self.new_edit()
                occupied = self.snapshot(inspection, own_intent=True)
                if occupied:
                    if not published:
                        self.wait_published()  # own committed candidate: reads only
                    return False, True
                require(not published, "Published version hash cannot be verified")
                # No target code is present, maximum is below target and queues
                # are clear. Reuse verified signed bytes with a NEW durable edit
                # generation; no dummy source commit or version override needed.
                self.write_intent(path, inspection)
                return True, False
            self.snapshot(intent["editId"], own_intent=True)
            return False, False
        edit = self.new_edit()
        occupied = self.snapshot(edit)
        if occupied:
            require(self.published(), "Occupied target has no durable edit receipt; manual reconciliation required")
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
            self.api.request("POST", prefix + ":commit?changesInReviewBehavior=ERROR_IF_IN_REVIEW&changesNotSentForReview=true")
        except ApiError as error:
            # Never repeat commit or cancel somebody else's review.
            try:
                self.wait_published()
            except Stop:
                raise error from None
        self.wait_published()
        return self.receipt()

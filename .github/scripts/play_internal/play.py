"""Internal-only transactional delivery using fresh deployed-state readback."""
from pathlib import Path
import re
import time

from .policy import PACKAGE, TRACK, Stop, read_json, require, write_json
from .transport import ApiError, PlayApi


def version(value):
    require(type(value) in (str, int) and re.fullmatch(r"[1-9][0-9]*", str(value)) is not None, "Unexpected Play version code")
    return int(value)


class Publisher:
    def __init__(self, api, manifest, fresh, *, sleep=time.sleep, commit_started=None):
        self.api, self.manifest, self.fresh, self.sleep = api, manifest, fresh, sleep
        self.code, self.digest = manifest["versionCode"], manifest["signedSha256"]
        self.name = f"internal-{self.code}-{manifest['source']['sha'][:12]}"
        self.commit_started = commit_started or (lambda intent: None)

    def new_edit(self):
        self.fresh()
        edit = self.api.request("POST", "/edits", {})
        require(re.fullmatch(r"[A-Za-z0-9_-]+", edit.get("id", "")) is not None, "Unexpected edit id")
        return edit["id"]

    def active(self, edit):
        data = self.api.request("GET", f"/edits/{edit}")
        require(data.get("id") == edit, "Saved edit identity differs from checkpoint")

    def bundle_match(self, edit):
        bundles = self.api.request("GET", f"/edits/{edit}/bundles").get("bundles", [])
        require(isinstance(bundles, list) and all(isinstance(item, dict) for item in bundles), "Invalid bundle list")
        candidates = [item for item in bundles if isinstance(item, dict) and version(item.get("versionCode")) == self.code]
        require(len(candidates) <= 1, "Ambiguous uploaded version")
        if candidates:
            require(isinstance(candidates[0].get("sha256"), str) and candidates[0]["sha256"].lower() == self.digest,
                    "Version code already occupied by different AAB bytes")
        return bool(candidates), bundles

    def snapshot(self, edit, *, own_intent=False):
        tracks = self.api.request("GET", f"/edits/{edit}/tracks").get("tracks")
        require(isinstance(tracks, list) and all(isinstance(item, dict) for item in tracks), "Invalid track snapshot")
        names = [item.get("track") for item in tracks]
        require(TRACK in names and all(isinstance(name, str) and name for name in names)
                and len(names) == len(set(names)), "Real Internal track missing or ambiguous")
        internal = self.api.request("GET", f"/edits/{edit}/tracks/internal")
        require(internal.get("track") == TRACK, "Unexpected Internal track response")
        codes = []
        for track in tracks:
            releases = track.get("releases", [])
            require(isinstance(releases, list), "Invalid track releases")
            for release in releases:
                require(isinstance(release, dict), "Invalid track release")
                items = release.get("versionCodes", [])
                require(isinstance(items, list), "Invalid track version codes")
                release_codes = [version(code) for code in items]
                own = own_intent and track["track"] == TRACK and release.get("name") == self.name and release_codes == [self.code]
                require(release.get("status") in ("completed", "inProgress", "halted")
                        or (own and release.get("status") == "draft"), "Known foreign draft or unknown track status; preserve pending changes")
                codes.extend(release_codes)
        occupied, bundles = self.bundle_match(edit)
        apks = self.api.request("GET", f"/edits/{edit}/apks").get("apks", [])
        require(isinstance(apks, list) and all(isinstance(item, dict) for item in apks), "Invalid APK list")
        codes.extend(version(item.get("versionCode")) for item in bundles + apks)
        require(not codes or max(codes) <= self.code, "A newer Play version exists; refusing downgrade")
        require(occupied or self.code not in codes, "Occupied version has no matching bundle")
        return occupied

    def validate_intent(self, intent):
        require(intent.get("schema") == 2 and intent.get("source") == self.manifest["source"], "Wrong authenticated edit checkpoint source")
        require(intent.get("signedSha256") == self.digest and intent.get("versionCode") == self.code
                and intent.get("track") == TRACK and intent.get("releaseName") == self.name, "Edit checkpoint differs from signed bytes")
        require(intent.get("phase") in ("prepared", "commit"), "Unknown checkpoint phase")
        require(re.fullmatch(r"[A-Za-z0-9_-]+", intent.get("editId", "")) is not None, "Invalid edit id")

    def write_intent(self, path, edit):
        write_json(path, {"schema": 2, "source": self.manifest["source"], "signedSha256": self.digest,
                          "versionCode": self.code, "track": TRACK, "releaseName": self.name,
                          "editId": edit, "phase": "prepared"})

    def track_matches(self, track):
        releases = track.get("releases", [])
        return (track.get("track") == TRACK and isinstance(releases, list) and len(releases) == 1
                and releases[0].get("name") == self.name and releases[0].get("versionCodes") == [str(self.code)]
                and releases[0].get("status") == "completed")

    def serving(self, edit):
        # A NEW edit copies DEPLOYED state, unlike the uncommitted staging edit.
        track = self.api.request("GET", f"/edits/{edit}/tracks/internal")
        require(track.get("track") == TRACK, "Wrong deployed track")
        if not self.track_matches(track):
            return False
        require(self.bundle_match(edit)[0], "Deployed version hash cannot be verified")
        return True

    def wait_served(self):
        for attempt in range(24):
            if self.serving(self.new_edit()):
                return
            if attempt < 23:
                self.sleep(5)
        raise Stop("Commit availability not proven by fresh deployed Internal completed/hash")

    def begin(self, path):
        path = Path(path)
        self.fresh()
        if path.exists():
            intent = read_json(path)
            self.validate_intent(intent)
            try:
                self.active(intent["editId"])
            except ApiError as error:
                require(error.status in (400, 404, 410), "Cannot read saved edit; keep checkpoint")
                inspection = self.new_edit()
                if self.serving(inspection):
                    return False, True
                if intent["phase"] == "commit" and self.commit_started(intent) is not False:
                    # Commit might have succeeded; never resubmit merely because
                    # store propagation is slow or its response was lost.
                    self.wait_served()
                    return False, True
                self.snapshot(inspection, own_intent=True)
                self.write_intent(path, inspection)
                return True, False
            self.snapshot(intent["editId"], own_intent=True)
            return False, False
        edit = self.new_edit()
        occupied = self.snapshot(edit)
        if occupied and self.serving(edit):
            return False, True
        require(not occupied, "Occupied version has no authenticated edit checkpoint")
        self.write_intent(path, edit)
        return True, False

    def stage(self, intent, bundle):
        self.validate_intent(intent)
        edit = intent["editId"]
        self.fresh()
        self.active(edit)
        occupied = self.snapshot(edit, own_intent=True)
        if not occupied:
            self.fresh()
            try:
                uploaded = self.api.request("POST", f"/edits/{edit}/bundles?uploadType=media", bundle, upload=True)
                require(version(uploaded.get("versionCode")) == self.code and uploaded.get("sha256", "").lower() == self.digest,
                        "Upload differs from sealed bytes")
            except ApiError:
                require(self.bundle_match(edit)[0], "Upload outcome unknown; no blind re-upload")
        self.fresh()
        self.active(edit)
        self.snapshot(edit, own_intent=True)
        current = self.api.request("GET", f"/edits/{edit}/tracks/internal")
        if not self.track_matches(current):
            self.fresh()
            target = {"track": TRACK, "releases": [{"name": self.name, "versionCodes": [str(self.code)], "status": "completed"}]}
            try:
                staged = self.api.request("PUT", f"/edits/{edit}/tracks/internal", target)
            except ApiError:
                staged = self.api.request("GET", f"/edits/{edit}/tracks/internal")
            require(self.track_matches(staged), "Internal staging not confirmed")

    def commit(self, intent):
        self.validate_intent(intent)
        require(intent["phase"] == "commit", "Commit checkpoint was not persisted after staging")
        edit = intent["editId"]
        self.fresh()
        self.active(edit)
        require(self.track_matches(self.api.request("GET", f"/edits/{edit}/tracks/internal")), "Staged target differs from commit checkpoint")
        require(self.bundle_match(edit)[0], "Staged bundle hash differs from commit checkpoint")
        # Console changes / another API commit invalidate this exact edit.
        # The server also atomically rejects an existing review. No other track,
        # listing, tester or review-cancellation endpoint is ever written.
        self.fresh()
        try:
            self.api.request("POST", f"/edits/{edit}:commit?changesInReviewBehavior=ERROR_IF_IN_REVIEW")
        except ApiError as failure:
            try:
                self.active(edit)
            except ApiError as state:
                require(state.status in (400, 404, 410), "Commit outcome unknown; retain checkpoint")
                self.wait_served()
                return self.receipt()
            raise failure
        self.wait_served()
        return self.receipt()

    def receipt(self, *, already=False):
        return {"outcome": "SERVED", "alreadyPublished": already, "package": PACKAGE, "track": TRACK,
                "status": "completed", "versionCode": self.code, "releaseName": self.name,
                "signedSha256": self.digest, "source": self.manifest["source"],
                "verification": "fresh-deployed-internal-completed-and-bundle-sha256", "lifecycleEnumRead": False}

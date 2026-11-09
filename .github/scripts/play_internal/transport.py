"""Bounded Publisher HTTP/JSON transport; diagnostic metadata never includes bodies."""
import json
from pathlib import Path
import re
import time
import urllib.error
import urllib.request

from .policy import PACKAGE, Stop, require

BASE = f"https://androidpublisher.googleapis.com/androidpublisher/v3/applications/{PACKAGE}"
UPLOAD = f"https://androidpublisher.googleapis.com/upload/androidpublisher/v3/applications/{PACKAGE}"
MAX_RESPONSE = 4_000_000


class NoRedirect(urllib.request.HTTPRedirectHandler):
    def redirect_request(self, request, response, code, message, headers, new_url):
        # Fixed Google endpoints must answer directly. In particular never
        # forward an explicit Authorization header to an unexpected location.
        return None


class ApiError(Stop):
    def __init__(self, status, detail=""):
        self.status = status
        super().__init__(f"Play API request failed (HTTP {status})" + (f": {detail}" if detail else "") + "; no automatic permission/review fallback")


def object_pairs(pairs):
    result = {}
    for key, value in pairs:
        if key in result:
            raise ValueError("Duplicate JSON key")
        result[key] = value
    return result


def response_metadata(response, data):
    mime = response.headers.get_content_type().lower()
    if not data.strip():
        shape = "empty"
    elif data.lstrip().startswith(b"<"):
        shape = "html-or-xml"
    else:
        shape = "non-json"
    try:
        value = json.loads(data, object_pairs_hook=object_pairs)
        shape = "object" if isinstance(value, dict) else type(value).__name__
    except (ValueError, UnicodeError):
        value = None
    return value, f"status={response.status} mime={mime} bytes={len(data)} shape={shape}"


def error_reason(value):
    if not isinstance(value, dict) or not isinstance(value.get("error"), dict):
        return ""
    error = value["error"]
    # Only machine enums / known parameter names, never the arbitrary message.
    reasons = [error.get("status", "")]
    items = [item for field in ("details", "errors") if isinstance(error.get(field), list) for item in error[field]]
    for item in items:
        if isinstance(item, dict):
            reasons.append(item.get("reason", ""))
    safe = [item for item in reasons if isinstance(item, str) and re.fullmatch(r"[A-Za-z][A-Za-z0-9_]{0,79}", item)]
    message = error.get("message", "")
    if isinstance(message, str) and "changesNotSentForReview" in message:
        safe.append("changesNotSentForReview-parameter")
    return " reasons=" + ",".join(safe) if safe else ""


class PlayApi:
    def __init__(self, token, *, sleep=time.sleep):
        require(bool(token), "Missing short-lived Play access token")
        self.token, self.sleep = token, sleep
        self.opener = urllib.request.build_opener(NoRedirect())

    def request(self, method, path, body=None, *, upload=False):
        data = Path(body).read_bytes() if upload else (json.dumps(body).encode() if body is not None else None)
        headers = {"Authorization": f"Bearer {self.token}", "Accept": "application/json"}
        if data is not None:
            headers["Content-Type"] = "application/octet-stream" if upload else "application/json"
        url = (UPLOAD if upload else BASE) + path + ("&" if "?" in path else "?") + "alt=json"
        if method == "GET" and re.fullmatch(r"/tracks/[^/]+/releases", path):
            url += "&%24outputDefaults=true"
        # Only GETs retry transport failures. A mutation may already have taken
        # effect even when its response is missing/malformed: reconcile it.
        attempts = 3 if method == "GET" else 1
        for attempt in range(attempts):
            try:
                request = urllib.request.Request(url, data=data, headers=headers, method=method)
                with self.opener.open(request, timeout=180) as response:
                    raw = response.read(MAX_RESPONSE + 1)
                    value, metadata = response_metadata(response, raw)
                    valid_mime = response.headers.get_content_type().lower() == "application/json"
                    if response.status not in (200, 201) or len(raw) > MAX_RESPONSE or not valid_mime or not isinstance(value, dict) or "error" in value:
                        raise ApiError(response.status, f"{method} {path}: invalid JSON object response; {metadata}")
                    if "/releases" in path:
                        print(f"Play response {method} {path}: {metadata}")
                    return value
            except urllib.error.HTTPError as error:
                with error:
                    value, metadata = response_metadata(error, error.read(MAX_RESPONSE + 1))
                    failure = ApiError(error.code, f"{method} {path}: {metadata}" + error_reason(value))
                if error.code not in (429, 500, 502, 503, 504) or attempt == attempts - 1:
                    raise failure from None
            except ApiError:
                if attempt == attempts - 1:
                    raise
            except (urllib.error.URLError, TimeoutError):
                if attempt == attempts - 1:
                    raise ApiError(0, f"{method} {path}: transport failure") from None
            self.sleep(2 ** attempt)

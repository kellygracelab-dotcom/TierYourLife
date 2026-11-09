# Automatic Google Play Internal delivery

A successful **push-main CI** starts `.github/workflows/play-internal.yml` through
`workflow_run`. It signs the exact release AAB produced by that CI attempt and
publishes `com.artiuillab.tieryourlife` to **internal**, status **completed**.
There is no input for another package, track or status. Production remains a
manual decision by Danylo. This workflow never installs anything on his phone.

The existing Android CI gates remain mandatory. For a code change this includes
build/unit/lint/static checks, screenshots/coverage, the R8 release bundle, proxy
E2E and instrumentation on API 26 **and** 34. Docs-only skips require a successful
gate receipt proving applicability. PRs, forks and manually dispatched CI cannot
obtain signing or Play credentials.

## One-time external setup

The coordinator owns GitHub/GCP/Play configuration and secret entry. No local
personal keystore or account configuration is read by these scripts.

Create environment **play-internal**, restricted to main, with these names:

| Kind | Name |
|---|---|
| Variable | `PLAY_WIF_PROVIDER` |
| Variable | `PLAY_PUBLISHER_SERVICE_ACCOUNT` |
| Secret | `PLAY_UPLOAD_KEYSTORE_BASE64` |
| Secret | `PLAY_UPLOAD_STORE_PASSWORD` |
| Secret | `PLAY_UPLOAD_KEY_ALIAS` |
| Secret | `PLAY_UPLOAD_KEY_PASSWORD` |
| Secret | `PLAY_CHECKPOINT_KEY` — 32 random bytes, base64, coordinator-generated |

Bind WIF to repository ID `1279175743`, owner ID `280666991`, `ref=refs/heads/main`,
`event_name=workflow_run`, and:

```text
workflow_ref=kellygracelab-dotcom/TierYourLife/.github/workflows/play-internal.yml@refs/heads/main
sub=repo:kellygracelab-dotcom/TierYourLife:environment:play-internal
```

The publisher needs app metadata read access and release access to testing tracks
for this package. **Do not grant production release access.** It must be able to
read the real track IDs, bundles/APKs and release lifecycles, including production
metadata. A denied read is a stop, not a reason to omit a guard. WIF creates a
short-lived access token with only the `androidpublisher` OAuth scope. No service
account JSON or credentials file is used.

The upload certificate is pinned to the public SHA-256 previously checked in the
signed 477 AAB:

```text
D7FE40A522B6B0DEEF7BC5049EC22466C62941A808539B3D7F2C5B79F75D69C4
```

Every non-signature payload entry must verify with exactly that signer. A fake
test key cannot publish. The temporary keystore has owner-only permissions,
passwords are passed by environment reference, tool errors are captured, and
cleanup runs even on signing failure. This repository is public: **no plaintext
signed AAB is uploaded as an Actions artifact**. The JDK encrypts it with
AES-256-GCM, a fresh 96-bit nonce and authenticated container version header.
Only `bundle.enc` and public provenance are uploaded. Decryption happens inside
the trusted publisher, after archive digest verification; plaintext is written
only after the entire container authenticates, then its hash, payload and actual
signer are reverified. The environment checkpoint secret is never an argument,
log or artifact. Preserve it while retained checkpoints are needed. No private
key, password or token is an artifact.

## Version and artifact provenance

CI checks out full Git history. `versionCode` is the exact source commit count,
without an override. The release uses the public production proxy address
`https://europe-west1-tieryourlife.cloudfunctions.net/`, tracked production Firebase
configuration and the existing release Play Integrity provider. The public
Firebase project/package/web-OAuth presence and file digest are checked without
printing API keys or OAuth client IDs.

Unsigned AAB provenance binds source SHA/tree, version, package, public inputs,
CI run/attempt, repository IDs and SHA-256. The privileged job independently
checks the source run, all applicable jobs, gate receipt, artifact archive digest,
inner AAB digest and decoded manifest. Archives may contain only the expected
data files; no artifact script is executed. Signing must preserve every original
payload byte. Main freshness is checked before signing/authentication and every
Play mutation. Superseded CI is skipped before checkout; an in-flight run is not
cancelled in the middle of upload/commit.

## Retry and availability

The signed AAB is saved **encrypted before** opening a Play edit. An edit-intent artifact is
saved **before** uploading a bundle or changing a track. Sealed bytes bind the
source SHA, with the original successful CI run/attempt recorded in provenance;
each intent generation additionally binds its CD run/attempt. Recovery trusts the artifact's creator workflow and
repository, not its name alone, and revalidates its digest and signature.

Rerun the failed **Play Internal** workflow while the same main remains current.
It restores the exact signed bytes and its own saved edit, and reads
bundle state before deciding whether an upload is needed. Lost upload/track/commit
responses are reconciled through reads; mutation requests are never blindly
retried. A newer Play code, a different AAB occupying the same code, missing
receipts, expired artifacts or unknown state stops delivery. If the saved edit
expired without consuming the target code, a read-only inspection must prove
that the code is free and queues are clear before creating a new intent generation
with the same sealed AAB. A committed pending candidate is polled without further
writes. Matching owned bytes already uploaded to an expired edit can be staged
through a new durable intent without another upload. The serving read snapshot
is created after publication is observed. Foreign queues are guarded before
release writes; a later foreign draft does not negate proven Internal delivery.
Re-running all CI jobs for an already delivered SHA reuses and verifies
the original signed artifact, original green CI attempt, bundle hash and published
release instead of signing/uploading again. A partial CI rerun missing its build
artifact must be rerun with all jobs. Do not override the count, downgrade or
re-upload an occupied code to force a retry.

Commit always specifies `changesInReviewBehavior=ERROR_IF_IN_REVIEW`; it never
cancels or replaces another review. Normal Internal commits omit
`changesNotSentForReview`: Google documents it for rejection handling, potentially
requiring manual Console submission, not as general isolation from metadata.
An actual rejection/permission/review error stops without automatic cancellation
or a switch to another track. Ordinary Internal delivery needs no manual gate.

All visible track release lifecycles are read before release writes. Foreign
draft, unsent, in-review, rejected, approved-but-unpublished or unknown releases
stop publication. The API does not expose every kind of unsent Console metadata
change and cannot atomically isolate this edit from manual Console work. **Pause
this workflow for the specific window in which someone prepares/submits manual
production or other Console changes; wait for an in-flight run to finish before
writing there.** Reading Console, normal pushes and ordinary Internal testing do
not require a pause. Resume after the manual queue is resolved. The coordinator
checks Publishing overview before the first automatic write.

Successful commit is not the delivery criterion. A separate
`applications.tracks.releases.list` read must show this exact release name and
version in `RELEASE_LIFECYCLE_STATE_PUBLISHED`. Since that state also includes
halted releases, a fresh read edit must independently confirm the currently
serving Internal release is `completed` with the exact name/version/bundle hash.
Missing/pending availability is reported as unfinished. The
published receipt records source and signed SHA-256. Store propagation and the
real Google-login check on the Play-installed Samsung remain separate runtime QA.

### HTTP response contract

Requests explicitly negotiate JSON (`Accept` and `alt=json`); release-list reads
also request Google's `$outputDefaults=true` so empty repeated proto fields can
be represented explicitly. GETs have no request body or Content-Type. Redirects
are rejected before Authorization can be forwarded. Expected resource responses
must be bounded JSON objects with an application/json MIME type. Empty bytes,
HTML/XML, null, arrays, duplicate JSON keys, API error objects and unknown list
schemas never mean an empty queue or successful publication. Logs contain only
method/path, status, MIME, byte count, body classification and safe machine error
enums; no response body, token or credential is logged.

Official Discovery revision20261105 defines ListReleaseSummariesResponse with
only an optional `releases` list: `{}` and `{"releases":[]}` are legitimate empty
JSON responses. The method has no pageToken/pageSize parameters, and returns at
most20 non-obsolete releases. Unexpected pagination or a saturated20-item list
stops because complete queue evidence is unavailable. Track IDs are taken from
the actual edit track list; the write target remains the fixed `internal` ID.

References: [Discovery](https://androidpublisher.googleapis.com/$discovery/rest?version=v3),
[JSON system parameters](https://docs.cloud.google.com/apis/docs/system-parameters).

## Validation

```sh
python3 -m unittest discover -s .github/scripts/tests -v
bash .github/scripts/check-workflows.sh
```

Tests use synthetic transport fixtures and a newly generated ephemeral test key.
They exercise forged/fork artifacts, wrong attempts, skipped required gates,
archive restrictions, actual JAR entry verification, stale source, monotonic
versions, pending production, interrupted upload/commit, durable-edit recovery
and the distinction between commit success and published availability. They do
not claim an actual Google Play publication.

Primary references: [workflow_run trust](https://docs.github.com/en/actions/reference/workflows-and-actions/events-that-trigger-workflows#workflow_run),
[WIF auth](https://github.com/google-github-actions/auth),
[Play edit concurrency](https://developers.google.com/android-publisher/concurrency-considerations),
[commit behavior](https://developers.google.com/android-publisher/api-ref/rest/v3/edits/commit),
[release availability](https://developers.google.com/android-publisher/api-ref/rest/v3/applications.tracks.releases/list).

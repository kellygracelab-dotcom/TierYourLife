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
cleanup runs even on signing failure. Only the signed AAB and public provenance
are uploaded as artifacts. No private key, password or token is an artifact.

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

The signed AAB is saved **before** opening a Play edit. An edit-intent artifact is
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
writes. Re-running all CI jobs for an already delivered SHA reuses and verifies
the original signed artifact, original green CI attempt, bundle hash and published
release instead of signing/uploading again. A partial CI rerun missing its build
artifact must be rerun with all jobs. Do not override the count, downgrade or
re-upload an occupied code to force a retry.

Commit always specifies `changesInReviewBehavior=ERROR_IF_IN_REVIEW`; it never
cancels or replaces another review. It also specifies `changesNotSentForReview=true`
without automatic fallback. Google documents the latter for rejection handling;
it is **not** general isolation from Console metadata changes. If Play rejects an
option, inspect the actual failure and preserve the pending queue before deciding
on a focused correction.

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
version in `RELEASE_LIFECYCLE_STATE_PUBLISHED`; the staged Internal release must
be `completed`. Missing/pending availability is reported as unfinished. The
published receipt records source and signed SHA-256. Store propagation and the
real Google-login check on the Play-installed Samsung remain separate runtime QA.

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

# Automatic Google Play Internal delivery

A successful **push-main CI** starts .github/workflows/play-internal.yml through
workflow_run. It signs the exact CI-produced release AAB and releases
com.artiuillab.tieryourlife to **internal**, status **completed**.
Production stays Danylo's manual decision. There is no selectable package/track
input. This workflow never installs anything on his phone.

Every existing Android CI gate remains mandatory. Code changes require build,
unit/lint/static checks, screenshots/coverage, the R8 release AAB, proxy E2E and
instrumentation on API26 and34. Docs-only skips require a gate receipt proving
applicability. PR/fork/manual-CI events cannot obtain signing or Play credentials.

## External setup

The coordinator owns GitHub/GCP/Play setup and secret entry. Never retrieve
private keys or account configuration from the local computer.

Environment **play-internal**, main only:

| Kind | Name |
|---|---|
| Variable | PLAY_WIF_PROVIDER |
| Variable | PLAY_PUBLISHER_SERVICE_ACCOUNT |
| Secret | PLAY_UPLOAD_KEYSTORE_BASE64 |
| Secret | PLAY_UPLOAD_STORE_PASSWORD |
| Secret | PLAY_UPLOAD_KEY_ALIAS |
| Secret | PLAY_UPLOAD_KEY_PASSWORD |
| Secret | PLAY_CHECKPOINT_KEY — 32 random bytes, base64 |

WIF is bound to repository ID1279175743, owner ID280666991,
ref=refs/heads/main, event_name=workflow_run, and:

    workflow_ref=kellygracelab-dotcom/TierYourLife/.github/workflows/play-internal.yml@refs/heads/main
    sub=repo:kellygracelab-dotcom/TierYourLife:environment:play-internal

The service account needs app-information read and testing release access for
this package. **Do not grant production release, store-presence or account
administration permissions.** The token is short-lived, scoped to androidpublisher;
no service-account JSON or credentials file is used.

The upload certificate SHA256 is pinned:

    D7FE40A522B6B0DEEF7BC5049EC22466C62941A808539B3D7F2C5B79F75D69C4

Every non-signature payload entry must verify with exactly this signer. Passwords
are passed by environment reference. The temporary keystore is owner-only and
removed in finally. Fake test keys cannot publish.

## Source and protected checkpoints

CI uses full history; versionCode is the exact main commit count, without an
override. The release explicitly uses the public production proxy URL
https://europe-west1-tieryourlife.cloudfunctions.net/ and tracked production
Firebase configuration, with the existing release Play Integrity provider.

Provenance binds SHA/tree/count/package/configuration digests and CI run/attempt.
The publisher verifies actual successful applicable jobs, gate receipt, artifact
archive and AAB digests, decoded manifest and original unsigned payload. Stale
sources stop before release writes. Only the guarded current-main environment
job has cache-mode:write; workflow_run otherwise has read-only cache access.

This public repository never stores plaintext signed AABs, signing keys, passwords
or tokens in Actions artifacts/caches. AES-256-GCM authenticates an encrypted
checkpoint containing signed and original unsigned AABs, provenance and the
original successful gate receipt. Public provenance must equal its authenticated
copy. This also preserves the original inputs if rerunning CI deletes old
unsigned artifacts. An encrypted intent binds edit ID, source, exact signed hash,
target and phase. The environment encryption key is never a command argument.

**Run artifacts are audit copies, not retry storage.** Actual full rerun480
deleted the prior signed/intent artifacts. Recovery uses source-bound Actions
cache entries and authenticated intent generations. Before Play operations, and
before each mutation phase, the workflow saves the checkpoint, moves its local
folder aside, independently restores the exact key into an empty original path,
and verifies cache-hit, file digests, authentication and semantics. A warning-only
cache save or residual local file cannot falsely confirm persistence.

Caches remain best-effort storage: GitHub can evict them after7days without access
or under quota pressure. No cache/storage/billing limit is raised. Missing
checkpoints after a possibly started release stop before signing or re-upload.
Historical job steps are a negative uncertainty guard, never proof that a Play
version is free. Actual Play version/hash checks remain mandatory before upload.
Never force a different AAB over an occupied version or override commit count.

## Transaction and serving evidence

The workflow separates:
1. Create/recover an edit and persist a prepared intent.
2. Upload once or reuse the exact already uploaded bundle, and stage only internal.
3. Persist a commit-intent checkpoint before the single commit request.
4. Commit with changesInReviewBehavior=ERROR_IF_IN_REVIEW and confirm delivery.

There is no review cancellation or automatic permission/track fallback.
Normal Internal commits omit the rejection-specific changesNotSentForReview flag.
Draft/unknown statuses in the available edit track snapshot stop release writes.
Console changes or another API commit invalidate the active edit; the exact edit
is checked again before commit. An in-review transaction is rejected by the
server. No redundant calls to other tracks' release-lifecycle endpoints are made;
a forbidden endpoint is never interpreted as an empty queue.

A successful commit is insufficient. A **new** read edit copies the current
**deployed** state. It must show the exact Internal name/version as completed,
and the bundle list must have the exact signed SHA256. Official Status.completed
means those APKs are being served to users. The receipt records outcome=SERVED
and this verification method; it does **not** claim the separate lifecycle enum
RELEASE_LIFECYCLE_STATE_PUBLISHED was queried. Staging alone, halted/wrong versions,
missing hashes or delayed availability cannot produce a success receipt.

Reads can retry; mutation requests are never blindly repeated. A lost upload
response is reconciled in the same edit. A lost commit response is reconciled
through actual edit state and fresh deployed readback. Expired prepared edits can
reuse verified owned bytes with a new authenticated intent. For an expired
commit marker whose specific Commit step demonstrably never started, recovery
can resume; missing/ambiguous history stays conservative. Store propagation and
the real Samsung Google-login check remain the coordinator's separate runtime QA.

## Hold for manual Console changes

The Publishing API can send pre-existing ready Console changes for review.
Neither a track snapshot nor ERROR_IF_IN_REVIEW fully enumerates unsent listing,
data-safety and other metadata. Do not claim that those are all visible to this
API. Use a **specific manual-write window**, not a confirmation for each release.

Before preparing manual production/other Console changes, the owner disables
**only Play Internal**:

    gh workflow disable play-internal.yml -R kellygracelab-dotcom/TierYourLife

Then wait for **all existing Play Internal runs/jobs** (including queued/waiting)
to finish before leaving ready changes in Console. Disabling does not cancel an
in-flight edit. Inspect active runs in Actions or:

    gh run list -R kellygracelab-dotcom/TierYourLife --workflow play-internal.yml --limit 100 --json databaseId,status,url --jq '.[] | select(.status != "completed")'

CI, Web/Proxy delivery and other schedules are unaffected. After the owner
resolves the manual queue, re-enable:

    gh workflow enable play-internal.yml -R kellygracelab-dotcom/TierYourLife

If the latest main CI finished while this workflow was disabled, rerun that CI
to emit its completion event; do not make a dummy commit. If a matching CD run
already exists for the still-current CI attempt, rerun that CD instead. Cache
loss fails closed. Normal pushes and ordinary Internal testing need no manual
hold or confirmation.

## Validation

    python3 -m unittest discover -s .github/scripts/tests -v
    bash .github/scripts/check-workflows.sh

Tests cover authenticated complete checkpoints and intents, deleted-artifact
recovery assumptions, missing/partial cache readback, stage versus deployed
state, occupied/different versions, stale edits, review rejection, lost responses,
no duplicate uploads and exact serving hashes. Synthetic tests do not prove an
actual Play release or durable provider storage.

Actionlint1.7.12 predates GitHub's official job cache-mode key. workflow_lint.py
independently permits exactly cache-mode:write in the protected Play publish job,
replaces only that validated line with a same-line comment for actionlint, and
passes all other text unchanged. No broad syntax ignore or app-gate reduction.

Primary contracts: [Edits](https://developers.google.com/android-publisher/edits),
[track status](https://developers.google.com/android-publisher/api-ref/rest/v3/edits.tracks),
[commit](https://developers.google.com/android-publisher/api-ref/rest/v3/edits/commit),
[Console concurrency](https://developers.google.com/android-publisher/concurrency-considerations),
[cache limits/access](https://docs.github.com/en/actions/reference/workflows-and-actions/dependency-caching),
[cache-mode syntax](https://docs.github.com/en/actions/reference/workflows-and-actions/workflow-syntax#jobsjob_idcache-mode).

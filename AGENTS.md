# TierYourLife Android

Read the product rules in `../AGENTS.md` when present, `README.md` and the
applicable architecture/testing documents before changing code. Preserve the
existing UI/domain/data boundaries and use existing components and design tokens.

## Completing ordinary changes

Danylo's standing instruction of 2026-11-08 is to finish authorized changes
through delivery, rather than leave tested work on a branch:

1. Make the focused change and meaningful regression checks. Run the applicable
   local gates; `.github/workflows/ci.yml` defines the complete merge gates.
2. Push a PR to `main` and wait for all applicable build/unit/lint/static,
   screenshot/coverage, release bundle, end-to-end and API 26/34 instrumentation
   checks. Verify the final PR SHA. Do not bypass failures or reduce coverage.
3. Merge the green PR, update this canonical checkout to `origin/main`, and
   verify the source tree and main checks. APK provenance must identify main
   and its versionCode; rebuild/recheck when merge changes executable inputs.
4. For an authorized connected test phone, install the checked APK over the
   existing package using an update install. Verify package version and launch.
   Never uninstall, clear user data, downgrade or change an account to make an
   install pass. Keep the tested APK and hashes in `../qa-results` on D:.
5. Delete only completed, verified merged branches on origin and locally.
   Inspect local leftovers before deleting exact, resolved paths inside this
   repository. Preserve unrelated work, credentials, backups and QA evidence;
   never use a blanket `git clean -xfd`.
6. Update the local coordinator log and report PR, main SHA, CI, APK/version,
   actual device result and cleanup. Distinguish emulator/synthetic tests from
   real sign-in, production and physical-device checks.

The existing CI and commit-count versionCode mechanism remain authoritative;
verify that the delivered version is greater than the installed version before
updating. `fast` is the R8, debug-signed sideload variant and must not reach Play.
Google Play submission is a separate decision. Do not read .env, credentials,
upload keys or account configuration, and do not weaken CI or security settings.

## Existing command path

Replace `PR` with the accepted PR number. Inspect `gh pr checks PR --repo
kellygracelab-dotcom/TierYourLife`. With required-check protection and auto-merge
enabled, queue accepted future PRs using `gh pr merge PR --repo
kellygracelab-dotcom/TierYourLife --auto --squash`, then wait for
`gh pr view PR --json state,mergeCommit` to confirm `MERGED`.

Update the canonical checkout with `git -C D:/MyProjects/TierYourLife/TierYourLife
fetch --prune origin`, `git -C D:/MyProjects/TierYourLife/TierYourLife switch main`
and `git -C D:/MyProjects/TierYourLife/TierYourLife pull --ff-only`, preserving
any separate local work. Verify main CI and the final source SHA. In this
repository run `./gradlew.bat :app:assembleFast --max-workers=2 --console=plain`
for the R8 sideload build. A version override must be recorded explicitly and
must not lower the installed version.

For the connected Samsung, first verify `adb devices -l` identifies SM-F741B at
R5CXC22CC4K and read the installed package version with `adb -s R5CXC22CC4K shell
dumpsys package com.artiuillab.tieryourlife`. Inspect only relevant version,
install-time and UID fields. Copy `app/build/outputs/apk/fast/app-fast.apk` into
`../qa-results`, compare size/SHA-256, and run `apksigner verify` and `aapt dump
badging` using the installed SDK build-tools. Install the verified permanent
copy using `adb -s R5CXC22CC4K install -r APK_PATH` without uninstall/clear/-d.
Recheck the package version and launch with `adb -s R5CXC22CC4K shell am start
-W -n com.artiuillab.tieryourlife/.MainActivity`. Use the Android CLI skill to
inspect the app UI and confirm it is running; do not change real user data.
If the phone is disconnected, keep the checked APK and record installation as
pending until it reconnects. Save provenance and actual device evidence on D:.

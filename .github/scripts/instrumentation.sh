#!/usr/bin/env bash
# The instrumentation run, with one retry for two specific mishaps: the
# emulator finishing a module's tests with no results file ("296/296
# completed, 0 failed" and a missing XML), and the API 24 image dropping an
# install session ("Failed to install-write all apks"). A failing test fails
# twice and is reported the first time, so this cannot turn a real failure green.
#
# A file rather than an inline workflow script: the emulator runner runs an
# inline script one line per shell, so a variable set on one line is gone by the next.
set -u

adb shell settings put global verifier_verify_adb_installs 0

# The test runner's own lines stream into the job log as the run goes, so a
# hang names the test it stopped on instead of leaving an hour of silence.
adb logcat -c
adb logcat -s TestRunner:I &
logcat_pid=$!
trap 'kill "$logcat_pid" 2>/dev/null' EXIT

# Read straight off the pipeline, not after an `if`: a failed `if` with no `else`
# is a success, and `status=$?` there reported every red run green.
log=instrumentation.log
./gradlew connectedDebugAndroidTest 2>&1 | tee "$log"
status=${PIPESTATUS[0]}

if [ "$status" -eq 0 ]; then
  exit 0
fi

if grep -q "Could not load test results" "$log"; then
  echo "Retrying: a results file went missing, which is the emulator losing a report rather than a test failing."
elif grep -q "Failed to install-write all apks" "$log"; then
  echo "Retrying: the emulator dropped an install session before any test ran."
else
  exit "$status"
fi

./gradlew connectedDebugAndroidTest

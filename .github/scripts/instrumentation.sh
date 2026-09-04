#!/usr/bin/env bash
# The instrumentation run, with one retry for two specific mishaps.
#
# Every so often the emulator finishes a module's tests and the results file
# never appears, so Gradle fails a run in which every test passed: "296/296
# completed, 0 failed" and then a missing XML. It lands on a different module
# each time, which is what gives it away as the report going astray rather
# than a test going red.
#
# The API 24 image has a second one: "Failed to install-write all apks", an
# install session the emulator drops before a single test has run. Three
# times in one day on pull requests that changed only documentation, and
# each time a manual re-run went straight through.
#
# Retried on those two symptoms and no other. A failing test fails twice and
# is reported the first time, so this cannot turn a real failure green.
#
# In a file rather than inline in the workflow because the emulator runner
# runs an inline script one line per shell: a variable set on one line is
# gone by the next, and `exit $status` then exits with whatever the previous
# command happened to return.
set -u

adb shell settings put global verifier_verify_adb_installs 0

# Read straight off the run and before anything else, not after an `if`. A
# failed `if` with no `else` is itself a success, so `status=$?` there read 0
# and this script reported every red run green -- which it did, for two
# merges, with a genuinely failing test in both.
log=instrumentation.log
./gradlew connectedDebugAndroidTest > "$log" 2>&1
status=$?
cat "$log"

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

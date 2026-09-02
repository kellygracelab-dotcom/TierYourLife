#!/usr/bin/env bash
# The instrumentation run, with one retry for one specific mishap.
#
# Every so often the emulator finishes a module's tests and the results file
# never appears, so Gradle fails a run in which every test passed: "296/296
# completed, 0 failed" and then a missing XML. It lands on a different module
# each time, which is what gives it away as the report going astray rather
# than a test going red.
#
# Retried on that symptom and no other. A failing test fails twice and is
# reported the first time, so this cannot turn a real failure green.
#
# In a file rather than inline in the workflow because the emulator runner
# runs an inline script one line per shell: a variable set on one line is
# gone by the next, and `exit $status` then exits with whatever the previous
# command happened to return.
set -u

adb shell settings put global verifier_verify_adb_installs 0

log=instrumentation.log
if ./gradlew connectedDebugAndroidTest > "$log" 2>&1; then
  cat "$log"
  exit 0
fi

status=$?
cat "$log"

if ! grep -q "Could not load test results" "$log"; then
  exit "$status"
fi

echo "Retrying: a results file went missing, which is the emulator losing a report rather than a test failing."
./gradlew connectedDebugAndroidTest

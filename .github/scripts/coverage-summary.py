"""Turns Kover's XML into a few lines for the GitHub job summary."""
import re
import sys

try:
    xml = open(sys.argv[1], encoding="utf-8").read()
except OSError:
    print("Coverage: no report was written.")
    sys.exit(0)

total = xml[xml.rindex("</package>"):]
print("### Coverage of the logic by JVM tests\n")
print("| | Covered |")
print("|---|---|")
for kind, missed, covered in re.findall(r'<counter type="(\w+)" missed="(\d+)" covered="(\d+)"/>', total):
    if kind in ("LINE", "BRANCH", "METHOD"):
        missed, covered = int(missed), int(covered)
        print(f"| {kind.lower()} | {100 * covered / (missed + covered):.1f}% ({covered}/{missed + covered}) |")
print("\nScreens, generated code and DAOs are measured by the emulator tests, not here.")

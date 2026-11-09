"""Validate one official cache-mode key that actionlint1.7.12 predates.

All other workflow text is passed unchanged to the pinned actionlint binary.
https://docs.github.com/en/actions/reference/workflows-and-actions/workflow-syntax#jobsjob_idcache-mode
"""
from pathlib import Path
import re
import sys


def normalize(filename, source):
    section, job, count = None, None, 0
    lines = []
    for line in source.splitlines(keepends=True):
        if re.match(r"^[A-Za-z][A-Za-z0-9_-]*:", line):
            section = line.split(":", 1)[0]
            job = None
        match = re.match(r"^  ([A-Za-z0-9_-]+):\s*$", line)
        if section == "jobs" and match:
            job = match[1]
        if line.strip().startswith("cache-mode:"):
            if not (Path(filename).name == "play-internal.yml" and section == "jobs" and job == "publish"
                    and line.rstrip() == "    cache-mode: write" and count == 0):
                raise ValueError("Unsupported cache-mode placement/value; only protected Play publish job may write")
            count += 1
            line = "    # cache-mode: write validated independently for actionlint1.7.12\n"
        lines.append(line)
    if Path(filename).name == "play-internal.yml" and count != 1:
        raise ValueError("Protected Play cache write mode is required for workflow_run persistence")
    return "".join(lines)


if __name__ == "__main__":
    try:
        sys.stdout.write(normalize(sys.argv[1], Path(sys.argv[1]).read_text(encoding="utf-8")))
    except ValueError as error:
        raise SystemExit(str(error)) from None

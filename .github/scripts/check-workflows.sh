#!/usr/bin/env bash
set -euo pipefail
lint_dir=$(mktemp -d)
trap 'rm -rf -- "$lint_dir"' EXIT
curl --fail --silent --show-error --location \
  https://github.com/rhysd/actionlint/releases/download/v1.7.12/actionlint_1.7.12_linux_amd64.tar.gz \
  --output "$lint_dir/actionlint.tar.gz"
echo "8aca8db96f1b94770f1b0d72b6dddcb1ebb8123cb3712530b08cc387b349a3d8  $lint_dir/actionlint.tar.gz" | sha256sum --check --status
tar -xzf "$lint_dir/actionlint.tar.gz" -C "$lint_dir" actionlint
"$lint_dir/actionlint" -shellcheck= .github/workflows/*.yml

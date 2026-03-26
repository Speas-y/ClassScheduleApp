#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$ROOT"

git add -A
if git diff --cached --quiet; then
  echo "没有需要提交的更改。"
  exit 0
fi

MSG="${1:-更新 $(date '+%Y-%m-%d %H:%M')}"
git commit -m "$MSG"
git push

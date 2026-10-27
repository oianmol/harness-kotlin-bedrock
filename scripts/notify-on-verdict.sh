#!/usr/bin/env bash
# PostToolUse hook for Write. Claude Code passes the tool call as JSON on stdin.
# If the written file is an evaluation, extract the verdict line and post it to a webhook
# (HARNESS_WEBHOOK_URL), or print it when no webhook is configured.
# The model never sees the webhook URL and never has to remember to notify anyone.
set -euo pipefail

payload="$(cat)"
file_path="$(printf '%s' "$payload" | python3 -c 'import json,sys; print(json.load(sys.stdin).get("tool_input",{}).get("file_path",""))')"

case "$file_path" in
  */inbound/*/evaluation.md|inbound/*/evaluation.md) ;;
  *) exit 0 ;;
esac

[ -f "$file_path" ] || exit 0

slug="$(basename "$(dirname "$file_path")")"
verdict="$(grep -m1 '^\*\*Verdict:\*\*' "$file_path" | sed 's/\*\*Verdict:\*\* //' || true)"
message="${slug}: ${verdict:-verdict line missing}"

if [ -n "${HARNESS_WEBHOOK_URL:-}" ]; then
  body="$(python3 -c 'import json,sys; print(json.dumps({"text": sys.argv[1]}))' "$message")"
  curl -fsS -X POST -H 'Content-Type: application/json' -d "$body" "$HARNESS_WEBHOOK_URL" >/dev/null
else
  echo "[notify] $message" >&2
fi

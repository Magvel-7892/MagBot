#!/usr/bin/env bash
# Run from a second Termux session while start-magbot.sh is running.
# This checks inference only. It cannot create an alarm, note, event or message.
set -euo pipefail
KEY_FILE="$HOME/magbot-runtime/api-key"
[ -r "$KEY_FILE" ] || { echo 'Start the MagBot server first.' >&2; exit 1; }
KEY="$(cat "$KEY_FILE")"
printf 'Server health:\n'
curl --fail --silent --show-error --connect-timeout 5 --max-time 15 \
    http://127.0.0.1:8080/health
printf '\n\nLocal JSON inference test (no Android action will be executed):\n'
curl --fail --silent --show-error --connect-timeout 5 --max-time 180 \
    http://127.0.0.1:8080/v1/chat/completions \
    -H 'Content-Type: application/json' -H "Authorization: Bearer $KEY" \
    --data-binary '{"model":"magbot-local","temperature":0,"max_tokens":160,"stream":false,"response_format":{"type":"json_object"},"messages":[{"role":"system","content":"Return only a JSON object with keys actions and reply. Translate timer requests into actions with tool set_timer and args containing seconds (integer) and label (string). Do not reason aloud. Actions are proposals only."},{"role":"user","content":"Prepare a one minute timer labelled Demo."}]}'
printf '\n\nLook for seconds:60 in the JSON message content. This is not an action-execution test.\n'

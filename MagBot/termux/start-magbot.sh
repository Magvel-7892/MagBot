#!/usr/bin/env bash
set -euo pipefail
BASE="$HOME/magbot-runtime"
SERVER="$BASE/llama.cpp/build/bin/llama-server"
MODEL="${MAGBOT_MODEL:-$BASE/models/nemotron-4b-q4.gguf}"
KEY="$BASE/api-key"
[ -x "$SERVER" ] || { echo 'Run setup-termux.sh first.' >&2; exit 1; }
[ -f "$MODEL" ] || { echo "Model file is missing: $MODEL" >&2; exit 1; }
[ "$(head -c 4 "$MODEL")" = 'GGUF' ] || { echo 'The model is not a GGUF file.' >&2; exit 1; }
umask 077
if [ ! -s "$KEY" ]; then
    od -An -N24 -tx1 /dev/urandom | tr -d ' \n' > "$KEY"
    printf '\n' >> "$KEY"
fi
chmod 600 "$KEY"
printf '\nMagBot runs only on http://127.0.0.1:8080\n'
printf 'Local API key - paste into MagBot; do not share it:\n'
cat "$KEY"
printf '\nKeep this session open. Use Ctrl+C to stop the model.\n'
printf 'CPU only | 4 inference threads | 2048 context | 1 request at a time\n\n'
if command -v termux-wake-lock >/dev/null; then termux-wake-lock || true; fi
trap 'if command -v termux-wake-unlock >/dev/null; then termux-wake-unlock || true; fi' EXIT
"$SERVER" \
    --model "$MODEL" \
    --alias magbot-local \
    --host 127.0.0.1 --port 8080 \
    --api-key-file "$KEY" \
    --ctx-size 2048 --parallel 1 \
    --threads "${MAGBOT_THREADS:-4}" --threads-batch "${MAGBOT_THREADS:-4}" \
    --batch-size 128 --ubatch-size 128 \
    --n-gpu-layers 0 --no-context-shift \
    --jinja --reasoning-budget 0 \
    --chat-template-kwargs '{"enable_thinking":false}'

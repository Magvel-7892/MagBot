#!/usr/bin/env bash
set -u
BASE="$HOME/magbot-runtime"
printf 'Device / Android / ABI:\n'
getprop ro.product.model
getprop ro.build.version.release
getprop ro.product.cpu.abi
printf '\nMemory:\n'
free -h
printf '\nStorage:\n'
df -h "$HOME"
printf '\nllama.cpp commit:\n'
cat "$BASE/llama-commit.txt" 2>/dev/null || true
printf '\nModel files:\n'
ls -lh "$BASE/models"/*.gguf 2>/dev/null || true
printf '\nServer health:\n'
curl --silent --show-error --connect-timeout 3 --max-time 5 http://127.0.0.1:8080/health || true
printf '\nThis report intentionally does not print your API key or prompts.\n'

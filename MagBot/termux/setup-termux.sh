#!/usr/bin/env bash
# Run inside Termux, never with root. Downloads source/model; does not execute app actions.
set -euo pipefail
command -v pkg >/dev/null || { echo 'Run this script in the Android Termux app.' >&2; exit 1; }
BASE="$HOME/magbot-runtime"
SOURCE="$BASE/llama.cpp"
MODEL="$BASE/models/nemotron-4b-q4.gguf"
MODEL_URL='https://huggingface.co/nvidia/NVIDIA-Nemotron-3-Nano-4B-GGUF/resolve/main/NVIDIA-Nemotron3-Nano-4B-Q4_K_M.gguf'
EXPECTED_SHA='be5d9a656a51922f24f1f09a759cebb694e1f5d9728bf0ef9f8c972c5a0b5ef2'
HERE="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"

printf '\nMagBot: Termux + NVIDIA Nemotron 3 Nano 4B Q4_K_M\n'
printf 'This installs build packages and downloads about 2.84 GB of model weights.\n'
printf 'Keep roughly 8-10 GB free for the model and build files (planning allowance).\n'
printf 'Model and license: https://huggingface.co/nvidia/NVIDIA-Nemotron-3-Nano-4B-GGUF\n'
read -r -p 'Continue with this download and setup? [y/N] ' answer
case "$answer" in y|Y|yes|YES) ;; *) echo 'Setup cancelled; no changes made.'; exit 0 ;; esac

pkg update -y
pkg upgrade -y
pkg install -y git cmake ninja clang curl libandroid-spawn coreutils
mkdir -p "$BASE/models" "$BASE/bin"
if [ ! -d "$SOURCE/.git" ]; then
    git clone --depth 1 https://github.com/ggml-org/llama.cpp.git "$SOURCE"
fi
# Existing checkouts are intentionally NOT auto-updated. The built commit is recorded below.
cmake -S "$SOURCE" -B "$SOURCE/build" -G Ninja \
    -DCMAKE_BUILD_TYPE=Release \
    -DGGML_OPENMP=OFF -DGGML_VULKAN=OFF \
    -DLLAMA_CURL=OFF -DLLAMA_BUILD_TESTS=OFF -DLLAMA_BUILD_SERVER=ON
cmake --build "$SOURCE/build" --target llama-server -j2
git -C "$SOURCE" rev-parse HEAD > "$BASE/llama-commit.txt"

if [ ! -f "$MODEL" ]; then
    # Resume incomplete downloads. A bad hash never becomes the final model file.
    curl --fail --location --retry 3 --retry-delay 3 --continue-at - \
        --output "$MODEL.part" "$MODEL_URL"
    printf '%s  %s\n' "$EXPECTED_SHA" "$MODEL.part" | sha256sum --check -
    mv "$MODEL.part" "$MODEL"
else
    printf '%s  %s\n' "$EXPECTED_SHA" "$MODEL" | sha256sum --check -
fi
for script in start-magbot.sh smoke-test.sh diagnose.sh; do
    cp "$HERE/$script" "$BASE/bin/$script"
    chmod 700 "$BASE/bin/$script"
done
printf '\nSetup finished. Start with:\n  bash ~/magbot-runtime/bin/start-magbot.sh\n'
printf 'The model is downloaded once. Future starts do not need the Internet.\n'

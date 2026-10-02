# Primary references checked on 2026-10-02

These document the underlying model, APIs and tools. They do not certify this prototype's phone compatibility.

- [S1] NVIDIA official model card and file (GGUF size, llama.cpp use, license):
  https://huggingface.co/nvidia/NVIDIA-Nemotron-3-Nano-4B-GGUF
  https://huggingface.co/nvidia/NVIDIA-Nemotron-3-Nano-4B-GGUF/blob/main/NVIDIA-Nemotron3-Nano-4B-Q4_K_M.gguf
  Published file SHA-256: `be5d9a656a51922f24f1f09a759cebb694e1f5d9728bf0ef9f8c972c5a0b5ef2`.
- [S2] Official Termux install sources and source-mixing warning:
  https://github.com/termux/termux-app#installation
  https://f-droid.org/packages/com.termux/
- [S3] Official llama.cpp Android / Termux instructions:
  https://github.com/ggml-org/llama.cpp/blob/master/docs/android.md
- [S4] Official llama.cpp server API, JSON responses, authentication, context and reasoning controls:
  https://github.com/ggml-org/llama.cpp/blob/master/tools/server/README.md
- [S5] Android common intents: alarms, timers, calendar insertion:
  https://developer.android.com/guide/components/intents-common
- [S6] Android text sharing using ACTION_SEND / EXTRA_TEXT:
  https://developer.android.com/develop/ui/compose/sharing/send
  Google Keep API is described as an enterprise API; this prototype uses Android sharing, not that cloud API:
  https://developers.google.com/workspace/keep/api/guides
- [S7] Nothing support: background process restrictions and manufacturer/version-dependent battery settings:
  https://support.nothing.tech/hc/en-us/articles/33978409506705-Why-does-the-Nothing-X-App-frequently-disconnect-from-the-Watch
  This is general background-process guidance, not a verified screenshot-by-screenshot Nothing OS 4.1 menu path.
- [S8] Android Gradle Plugin 8.7 compatibility (Gradle 8.9, JDK 17 baseline, maximum SDK 35):
  https://developer.android.com/build/releases/agp-8-7-0-release-notes

Keep-targeted sharing and WhatsApp-targeted handoff still need testing against the versions installed on the user's phone. No private Google Keep content provider or undocumented direct-save mechanism is used.

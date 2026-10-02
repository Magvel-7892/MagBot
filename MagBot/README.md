# MagBot v0.2 - Nothing Phone (3a) + Termux + Google Keep

A text-only, local-model Android assistant prototype. Updated for the user's Nothing Phone (3a), 8 GB RAM / 128 GB storage, Nothing OS 4.1, Google Keep, and a separate Termux model server.

**Deliverable: source project and setup scripts, NOT an APK.** The policy layer has been compiled and tested on a desktop JVM. The full Android project has not been built here, the scripts have not been executed in Termux, and the model/app combination has not been tested on a physical phone. See `VALIDATION.md`. Do not treat compatibility or performance as established until the phone tests pass.

## What the prototype implements

The existing project remains native Android / Java, with no Flutter dependency.

| Command type | Implementation | Final user action |
|---|---|---|
| Alarm | Standard Android Clock intent, next occurrence of a time | Confirm in MagBot; verify in Clock |
| Timer | Standard Android timer intent | Confirm in MagBot; verify/start in Clock |
| Calendar event | Calendar insertion intent | Review calendar, dates and fields; tap Save |
| Google Keep note | Text sharing targeted at `com.google.android.keep` | Review the draft; tap Save |
| WhatsApp message | Share to WhatsApp, or click-to-chat for a supplied international number | Select/verify recipient; press Send |

These are implemented integration paths, not a claim that every receiving app version has been device-tested. If Keep cannot receive the intent, MagBot opens a notes-app chooser. Title handling is controlled by the receiving app; check it before saving.

This version does **not** read calendar events, read/search Keep notes, read chats, resolve contact names, listen for a wake word, or automatically press WhatsApp Send. It does not use accessibility services or root. It uses standard WhatsApp, not WhatsApp Business.

## Architecture

```text
MagBot app -> http://127.0.0.1:8080 -> llama-server in Termux
                                            |
                                  Nemotron 3 Nano 4B GGUF
                                            |
MagBot <- JSON proposal <- local inference <-+
   |
validate entire plan -> preview -> user confirms one action
   |
Clock / Calendar / Keep / WhatsApp
```

Multiple actions appear as separate cards. Open one, finish with the receiving app, return to MagBot, then confirm the next. They are deliberately not launched in a rapid loop.

## Model and initial runtime settings

- Official repository: `nvidia/NVIDIA-Nemotron-3-Nano-4B-GGUF`.
- File: `NVIDIA-Nemotron3-Nano-4B-Q4_K_M.gguf`, approximately 2.84 GB.
- Model license: NVIDIA Nemotron Open Model License. This is an open-weight model under NVIDIA's terms, not an Apache-2.0 claim. Review the model card before downloading.
- CPU backend only, four inference threads, context 2048, parallel requests 1, batch/microbatch 128.
- Reasoning output is disabled in the startup options to favor short JSON responses.
- The 2.84 GB file size is **not** peak RAM usage. Android, the model's state and runtime buffers also consume memory.
- These are conservative starting settings, not measured optimum settings or a speed guarantee for the Phone (3a). The app displays measured request latency after each successful plan.

The phone's NPU is not used by this CPU setup. Nothing OS 4.1 is recorded as user-provided; no assumption is made about its underlying Android API level.

## Setup order

### 1. Phone: install Termux

Install Termux from the F-Droid link referenced by the official Termux project [S2]. Do not mix Termux app/plugin packages from different sources. These scripts do not require the separate Termux:API plugin.

Long-press the Termux icon, open App info, and look for Battery / App battery usage. Allow background activity or select Unrestricted where available. Exact labels can vary on Nothing OS versions [S7]. Keep Termux open during the demo and do not swipe it out of recent apps. A wake lock cannot guarantee Android will never kill the process under memory pressure.

Keep approximately 8-10 GB free as a setup allowance for weights, compiler packages and build files. Use Wi-Fi for the first download.

### 2. Phone: extract this project and run setup

Download `MagBot-v0.2-Nothing.zip` to your phone's Downloads folder. Open Termux:

```bash
pkg update
pkg install unzip
termux-setup-storage
```

Grant the storage access requested by Android, then run:

```bash
mkdir -p ~/magbot-source
unzip ~/storage/downloads/MagBot-v0.2-Nothing.zip -d ~/magbot-source
bash ~/magbot-source/MagBot/termux/setup-termux.sh
```

The setup script asks before installing packages/downloading weights. It builds llama-server in Termux, verifies the downloaded model's SHA-256, and saves the model in Termux's private home directory. It records the actual llama.cpp commit in `~/magbot-runtime/llama-commit.txt`.

If Android renamed your downloaded ZIP, substitute its actual filename. The script clones the current llama.cpp source on first setup; it does not silently update an existing checkout.

### 3. Phone: start the server

```bash
bash ~/magbot-runtime/bin/start-magbot.sh
```

Copy the **local API key** printed in the terminal. This is generated on your phone; it is not an NVIDIA, Hugging Face or OpenAI API credential. Do not share it in screenshots or debugging messages.

Leave this session running. Wait for the server's ready/listening message. The server listens only on `127.0.0.1:8080`. Press Ctrl+C when you want to stop it. The script releases its wake lock on exit.

To test inference before building MagBot, open a second Termux session and run:

```bash
bash ~/magbot-runtime/bin/smoke-test.sh
```

The response should contain a proposed `set_timer` action with `seconds:60`. **This test does not set a timer**; it tests the model/API only. An HTTP success alone is not proof that the parsed task is correct.

### 4. Laptop: build and install the Android app

Extract the same ZIP on your laptop. Use Android Studio with Android SDK 35, a compatible JDK (17 is the documented baseline), Android Gradle Plugin 8.7.3 and Gradle 8.9 [S8]. There is no native Android library inside this APK, so this project does not require the NDK.

The ZIP omits the binary Gradle wrapper JAR. Bootstrap the official, pinned wrapper before importing the project:

**Windows PowerShell**, from the extracted `MagBot` folder:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\bootstrap-gradle.ps1
```

**Linux/macOS**, from the same folder:

```bash
bash tools/bootstrap-gradle.sh
```

These scripts download official Gradle 8.9 wrapper assets and compare the wrapper JAR to Gradle's published checksum. The PowerShell policy override applies only to that invocation.

Then open the `MagBot` folder in Android Studio, install any requested SDK components, sync, connect the phone with USB debugging, authorize that laptop, and press Run. Disable USB debugging afterward if you do not need it.

After a successful Gradle build, the debug APK path is `app/build/outputs/apk/debug/app-debug.apk`. No APK is included in this download. The laptop is needed to build/install the app, **not** to run inference during the demo.

### 5. Phone: connect MagBot

Open MagBot, paste the local API key, and tap **Check connection**. The app uses the fixed loopback endpoint; it cannot be pointed to a cloud URL through its UI.

Enter a simple request, tap **Prepare actions locally**, review the generated fields, then tap **Review and open action** and **Open app**. Re-check dates, recipients and messages in the destination app.

## Demo sequence

1. `Start a 1 minute timer called Demo`.
2. `Make a note titled Project: carry the project report`.
3. `Add Project Review tomorrow at 4 PM for 45 minutes`.
4. `WhatsApp Arun saying I will reach by 8` - choose Arun in WhatsApp yourself.
5. Put the phone in airplane mode and repeat the timer and Keep-note examples.

**Only model inference is guaranteed by design to target a local address.** Keep and a synced calendar can upload saved data according to their own account settings. WhatsApp delivery requires a network. Offline draft behavior is controlled by the installed app. No claim is made that these third-party apps are cloud-free.

## Safety and implementation details

The entire proposed plan is validated before action buttons are shown. Unsupported tools, unexpected argument keys, missing or wrongly typed fields, invalid times, excessively long text, an invented phone number, and more than six actions are rejected. JSON format is requested from llama.cpp; invalid, incomplete or prose-wrapped responses are rejected rather than scraped for commands.

Validation cannot prove that a small model understood your intent. Always inspect the confirmation preview. When the model asks a clarification question, enter a new complete command: v0.2 does not keep conversational history.

Android Clock's standard alarm intent cannot express an arbitrary future date. This implementation supports the **next occurrence** of the requested hour/minute and rejects an explicit incompatible date. Use a calendar event for later-day scheduling. Calendar times are interpreted in the phone's current timezone.

There is no automatic execution on receipt of model output and no automatic execution when returning from another app. After dispatch the action button is disabled, but MagBot cannot verify that another app really saved or sent anything. A rotation or process restart clears the pending plan; no action is automatically replayed.

The local API key is stored in app-private preferences; Android backup is disabled in the manifest. The app does not persist command history to disk, although unsent text can temporarily survive activity recreation through Android saved state. The server terminal can contain diagnostic output. Only INTERNET and the Clock SET_ALARM permission are requested; no Contacts, Calendar read/write, Accessibility or microphone permission is requested.

## Troubleshooting

| Symptom | Check |
|---|---|
| Connection refused | Start the server; ensure the phone is not killing Termux. |
| HTTP 401/403 | Paste the local key printed by the currently running script. |
| HTTP 503 | The model is loading or busy; check Termux output. |
| Unknown `nemotron_h` architecture | An old llama.cpp build may be in use. Record the commit, deliberately update the checkout and rebuild. |
| JSON/output rejected | Try one complete, simple command; check the startup script has reasoning disabled. No action was run. |
| Server killed | Check free RAM, keep Termux in foreground during testing, close other heavy apps. A smaller GGUF may be needed. |
| Phone gets hot | Stop the server and let it cool. Try `MAGBOT_THREADS=2 bash ~/magbot-runtime/bin/start-magbot.sh`. |
| Model checksum mismatch | Do not load it. Check the download and the official model revision; do not bypass verification blindly. |
| Keep opens a chooser | Check Keep is installed, enabled and has been opened at least once. Choose it manually if needed. |
| Calendar opens but no event exists | You must press Save in the calendar app. |
| Gradle wrapper missing | Run the laptop bootstrap script first. |

For a diagnostic report without your key or prompts:

```bash
bash ~/magbot-runtime/bin/diagnose.sh
```

See `SOURCES.md`, `TEST_CASES.md`, and `VALIDATION.md` for references and remaining checks.

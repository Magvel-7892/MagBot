# MagBot v0.2 device test checklist

These are **not yet executed on a phone**. The JVM-only results are in `tests/core-test-results.txt`.

## Local server and app

1. Start Termux server; `/health` returns OK after loading.
2. Run `smoke-test.sh`; inspect the final content for a proposed 60-second timer.
3. Build/install the APK from Android Studio. Confirm the UI avoids the status/navigation bars and keyboard.
4. Incorrect local key produces an authentication error; correct key connects.
5. Stop the server; app shows a connection error and opens no other app.
6. Turn on airplane mode; authenticated local model inference still works.
7. Keep the app open while a model request runs; verify actual latency and memory, not estimates.

## Commands and confirmation

| Command | Expected proposal / behavior |
|---|---|
| Start a one minute timer called Demo | `set_timer`, seconds=60; Clock only after confirmation |
| Set an alarm for 7 AM called Gym | `set_alarm`, hour=7/minute=0, next occurrence shown |
| Add Project Review tomorrow at 4 PM for 45 minutes | Start/end on the correct relative date in the phone timezone |
| Make a note titled Shopping: buy cement and paint | Keep draft; check both title and body; save yourself |
| WhatsApp Arun saying I will reach by 8 | WhatsApp recipient picker; no auto-resolved or invented number |
| WhatsApp +91 98765 43210 saying Hello | Number is preserved as supplied; verify recipient; never send automatically |
| Set an alarm for 6:30 AM and make a note to carry my report | Two separate action cards; execute individually |
| Add a meeting tomorrow | No action; ask for essential missing time; re-enter a complete command |
| Read my Keep notes | No action; feature unsupported |

Use only a real, authorized test recipient for the phone-number example; the sample number above is illustrative, not an invitation to contact it.

## Cancellation and failure tests

- Cancel the MagBot confirmation dialog: nothing opens and no task is created.
- Cancel in Keep/Calendar: MagBot must not claim anything was saved.
- Return from an external app: the other pending action is not launched automatically.
- Rotate/restart MagBot: no action is automatically replayed.
- Try a date beyond the next alarm occurrence: reject or request a calendar event, never silently set a different day.
- Ask for delete-all, shell execution, arbitrary app package or payment: reject unsupported tools.
- Check a named WhatsApp request does not invent a number and still requires contact selection.
- Try a long six-action request: either complete valid cards or fail safely on truncated output; no partial plan execution.
- Review Keep behavior when offline; distinguish local drafting from later Google sync.
- Run long enough to observe whether Nothing OS suspends Termux when switching between apps.

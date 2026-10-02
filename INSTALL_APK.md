# Install the test APK on your Nothing Phone (3a)

This file accompanies the intended GitHub build output. A successful build checks
packaging and signing; it does not prove that the app or model works on your phone.

1. Transfer or download `MagBot-v0.2-test.apk` onto the phone.
2. Open it using your browser's Downloads page or your file manager.
3. Android may ask you to allow **Install unknown apps** for that particular
   browser/file manager. Grant it only for this installation, then turn it off.
   Do not disable Play Protect. If a security warning blocks installation, stop
   and inspect the warning rather than bypassing it blindly.
4. Install and open MagBot. The app has no model bundled in it.
5. Start the separate local server in Termux using the v0.2 setup:

   ```bash
   bash ~/magbot-runtime/bin/start-magbot.sh
   ```

6. Paste the locally generated key into MagBot and tap **Check connection**.
   Do not post the key in screenshots or messages.
7. Try **Start a one minute timer called Demo**. Review the proposal, confirm,
   and verify the timer in Clock. Then test a Keep draft.

For first-time Termux setup, follow `MagBot/README.md` from the source package;
the included `MagBot/termux/setup-termux.sh` is the matching setup script.
If you already used the other v0.1 scripts that place the server under ~/magbot,
use that installation's startup command. The API key must be 16-128 letters,
digits, underscores or hyphens; the server must listen on 127.0.0.1:8080 and
accept the model alias `magbot-local`. Prefer the matching v0.2 setup for this APK.

**What is not included:** Termux, model weights, automatic server startup,
read/search access to Keep's private notes, or automatic WhatsApp sending.

A fresh CI run may sign with a different test key. If Android reports a signing
conflict, back up any relevant settings before removing an older MagBot test app
and installing this one. Uninstalling deletes that app's stored configuration.

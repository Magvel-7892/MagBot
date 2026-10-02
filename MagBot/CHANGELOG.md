# v0.2

- Added Google Keep-targeted sharing, with a notes-app chooser fallback.
- Replaced immediate execution with validated proposals, previews and individual confirmations.
- Enforced a fixed localhost endpoint, disabled HTTP redirects and added local API-key authentication.
- Added JSON-only output requests, incomplete-answer rejection and strict action argument validation.
- Added explicit next-occurrence alarm-date validation and future calendar-time validation.
- Rejects model-invented phone numbers not present in the user's command.
- Added connection checking and measured request latency.
- Added safe-area/keyboard insets handling to the UI.
- Removed use of newer String convenience APIs from the original source.
- Kept checked JSON exceptions in methods that explicitly propagate them.
- Added Termux setup, checksum-verified model download, startup, inference smoke test and diagnostics.
- Added bootstrap scripts for the previously missing Gradle wrapper.
- Added policy tests and explicit documentation of untested Android/model integration.
- Disabled Android backup in the app manifest.

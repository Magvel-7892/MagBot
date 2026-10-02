# Build MagBot's APK without Android Studio

**This download is not an APK.** It contains the original Nothing Phone / Google
Keep v0.2 source project plus a GitHub Actions workflow that is configured to build
and sign a test APK on a GitHub-hosted runner. The workflow has not been run here.

A local build was attempted on 2 October 2026. This environment has Java but no
Android SDK/Gradle installation; required tool downloads were blocked by its
network proxy. No APK was produced, and no phone or real-model tests were run.

## Steps

1. Sign in to GitHub and create a repository named `MagBot`. A private repository
   avoids publishing the code. Check your account's Actions quota/billing before
   running a build; this guide does not promise free or unlimited build minutes.
   Initialize the repository with a README to make the Upload files menu available.
2. Extract this ZIP. In the repository, use **Add file > Upload files**. Upload
   the *contents* of this extracted folder, including `.github`, `MagBot`,
   `README.md`, and `INSTALL_APK.md`. Do not upload the ZIP itself or nest
   everything inside an extra `MagBot-APK-Build` folder. Commit to `main`.
   Never add your local model access key, model weights, or signing keys.
3. Open **Actions > Build MagBot APK**. The upload triggers a build on `main`
   or `master`. You can also use **Run workflow** once the workflow file is
   present on the default branch. Actions must be enabled in the repository.
4. After a successful run, open its summary. Under **Artifacts**, download
   **MagBot-APK-test-build**. Extract that downloaded ZIP. It should contain
   `MagBot-v0.2-test.apk`, signature/alignment checks, a checksum and install notes.
5. Open that APK on your phone and follow `INSTALL_APK.md`.

If no workflow appears, check that the repository path is exactly:

```
.github/workflows/build-apk.yml
```

If the build fails, there is **no installable APK** until it is fixed. Open the
failed step and retain the error log. Do not rename a source ZIP to `.apk`.

## Scope of this version

The project is the original Nothing Phone / Keep v0.2 version, not the other
v0.1 package with MagBot's private notes database. It proposes alarm and timer
requests, calendar drafts, Keep note drafts and WhatsApp drafts after local
model interpretation. Every action requires confirmation. Save/Send is performed
in the destination app. No root or Accessibility service is used.

The workflow only builds app code; it does not download or run the model and
receives no phone prompts. The model stays in Termux on your phone. Keep,
WhatsApp and calendar synchronization have their own network behavior.

## Technical details and limits

- Android Gradle Plugin 8.7.3; Gradle 8.9; Java 17; compile/target SDK 35.
- Android SDK components are installed on GitHub's disposable runner. The setup
  step accepts their SDK licenses there; review Google's SDK terms before use.
- The build uses official GitHub actions and Gradle's official setup action.
  Action references are version tags, not immutable commit pins.
- The workflow runs the project's core checks, assembles a debug APK, and checks
  its signature and ZIP alignment. These do not establish phone compatibility.
- APK outputs are retained for seven days. Rebuild if the artifact expires.
- Each runner can create a different debug signing key. Replacing an older test
  installation may require uninstalling it first, which deletes app settings.
- Debug signing is for a prototype, not production distribution or Play release.
- No credentials or signing private keys are included in this package.

## References

- Android command-line APK builds and debug signing:
  https://developer.android.com/build/building-cmdline
- Gradle's setup action and pinned Gradle-version input:
  https://github.com/gradle/actions/blob/main/docs/setup-gradle.md
- Manually running a workflow:
  https://docs.github.com/en/actions/how-tos/manage-workflow-runs/manually-run-a-workflow
- Downloading GitHub Actions artifacts:
  https://docs.github.com/en/actions/how-tos/manage-workflow-runs/download-workflow-artifacts
- Android Studio / SDK terms:
  https://developer.android.com/studio

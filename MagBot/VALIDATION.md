# Validation status - 2026-10-02

## Executed in the authoring environment

- **37 JVM policy checks passed** using the actual `ActionValidator.java` implementation, plus an `AgentProtocol` prompt smoke check included in that count.
- Java source was compiled with Java 17 source/target settings using the OpenJDK 21 compiler module, then executed on the local JVM. This validates that tested Java logic; it is not Android SDK compilation or an Android API-availability audit.
- All six application Java source files passed compiler syntax parsing (no Android SDK type checking).
- All supplied shell scripts passed `bash -n` syntax checks.
- All Android XML files parsed successfully.
- Static checks verified a fixed localhost client, redirect disabling, API-key authentication, confirmation gating and expected minimum permissions.

Reproduce the JVM checks with `bash tests/run-core-tests.sh` on a laptop with Java/JDK 17+.

## Not executed / not delivered

- Full Gradle / Android compilation, lint and APK signing. This environment has no Android SDK; attempts to obtain SDK/build dependencies were blocked by network access restrictions.
- Therefore **no APK is included**, and the Android source must still be built in Android Studio.
- On-device UI, Clock, Calendar, Keep and WhatsApp integration tests.
- Running the setup/start scripts in actual Android Termux.
- Loading or benchmarking Nemotron on a Nothing Phone (3a).
- End-to-end tests of Nemotron reasoning-off plus JSON mode in this configuration.
- Executing the PowerShell/bootstrap downloads here.

The 37 passing policy checks do not prove that the model reliably understands commands, that the APK builds, or that all Android intent handlers behave identically across app versions. Use the supplied device checklist before presenting the demo.

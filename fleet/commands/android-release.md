---
description: Run the Android release pipeline — lint + full tests in parallel, then release build, optional signing/EAS submit, then smoke-verify the release APK on the emulator.
argument-hint: [--profile debug|release|preview|production] [--sign] [--bump patch|minor|major] [--track native|expo|auto]
---

# /android-release

Release-readiness pipeline. Delegates execution to `android-fleet-manager` so env + parse logic stays DRY.

## What this command does

1. Optional version bump (`versionName` + `versionCode` for native; `expo.version` + `android.versionCode` in `app.config.ts` for Expo).
2. Manager dispatches in **one parallel batch**:
   - `android-ci-agent` scope=`lint`
   - `android-ci-agent` scope=`unit-tests`
3. Once both pass, manager dispatches sequentially:
   - `android-ci-agent` scope=`assemble-release`
   - (if `--sign`) sign with credentials from env (`ANDROID_KEYSTORE_PATH`, `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS`, `ANDROID_KEY_PASSWORD`) — never inline secrets.
   - (Expo with `--profile production`) `eas build --platform android --profile production`.
4. `android-emulator-agent` installs the release artifact and smoke-launches.
5. Report: artifact path, size, signing status, lint count, test pass/fail, emulator verdict.

## Flags

- `--profile <name>` (default: `release`) — build profile (Gradle build type or EAS profile)
- `--sign` — sign the release artifact; requires env vars listed above
- `--bump patch|minor|major` — bump version before building
- `--track native|expo|auto` (default: `auto`)

## Examples

```
/android-release --profile release --sign
/android-release --profile production --sign --bump minor
/android-release --profile preview     # EAS preview build, unsigned
```

## Refusals

- Tests or lint failing → release pipeline halts; no build artifact produced.
- `--sign` without keystore env vars → refused with clear list of missing vars.
- Uncommitted changes when `--bump` requested → asks user to commit first.

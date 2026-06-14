# Android Dev Fleet

A fleet of Skills + Agents + Commands that orchestrate creating Android applications — across both **native Kotlin/Jetpack Compose** and **React Native + Expo** tracks. Spec-first, parallel-by-default, emulator-verified.

## What's inside

### Skills (`skills/`)
- **android-fleet-manager** — orchestration playbook the manager agent follows
- **android-native-scaffold** — bootstrap native Kotlin + Compose + Hilt + Room + Retrofit
- **expo-android-scaffold** — bootstrap Expo SDK 52 RN app mirroring `/Users/asharma52/github/hisabkitab`
- **android-compose-screen** — generate a Material 3 screen + ViewModel + UiState + nav + test

### Agents (`agents/`)
- **android-fleet-manager** — drives the pipeline; fans out independent agents in a single Agent batch
- **android-spec-writer** — produces `SPEC.md` (whole app) or `.specs/<feature>.md` (incremental); manager blocks until approved
- **android-ci-agent** — both-track build + test runner (lint, unit tests, instrumented, assemble); parallel-safe
- **android-emulator-agent** — boots AVD, installs APK, launches app, captures logcat + screenshot

### Commands (`commands/`)
- **/android-build** — full pipeline entrypoint (greenfield or large multi-feature)
- **/android-new-feature** — scoped feature pipeline
- **/android-release** — lint + tests + release build + optional signing + emulator verify

## Quickstart

```bash
# Greenfield: spec, scaffold, features, verify
/android-build "expense tracker like hisabkitab"

# Existing project: add a feature
/android-new-feature payments "P2P transfer with QR scan"

# Release readiness
/android-release --profile production --sign --bump minor
```

## Spec-first

No code is written before `SPEC.md` (or `.specs/<feature>.md`) exists and you approve it. The manager will run `android-spec-writer` first and pause.

## Parallel by default

When work is independent, the manager dispatches subagents in a single tool batch. Examples:
- Multiple feature screen-gens run concurrently
- `lint` and `unit-tests` run concurrently before `assemble`
- Emulator verify across API 30 / 33 / 34 can run concurrently

## Tracks

| Track | Use when | Reference |
|---|---|---|
| native-android | You want Kotlin, Compose, Hilt, Room | Modern Google-recommended stack |
| expo-react-native | You want one codebase for Android + iOS + web | `/Users/asharma52/github/hisabkitab` |

## Requirements

- JDK 17+
- Android SDK with `platforms;android-35`, `build-tools;35.0.0`, at least one system image (e.g., `system-images;android-34;google_apis;arm64-v8a`)
- `ANDROID_HOME` env var set
- Node 20+ and npm 10+ (for Expo track)

## See also

- `CLAUDE.md` — full conventions reference
- `.claude-plugin/plugin.json` — plugin manifest

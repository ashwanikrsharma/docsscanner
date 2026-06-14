---
name: android-native-scaffold
description: Scaffold a native Android app using Kotlin, Jetpack Compose, Hilt, Room, Retrofit, and Coroutines/Flow with Gradle KTS and a version catalog. Use when the user asks to create, bootstrap, or initialize a native Android / Kotlin / Jetpack Compose project. Not for React Native or Expo (use expo-android-scaffold for those).
---

# Android Native Scaffold

Bootstrap a production-grade native Android app following the modern Google-recommended stack.

## Pre-flight

- Require `SPEC.md` to exist and be approved. If missing, refuse and instruct the user to run `android-spec-writer` first (or use `/android-build`).
- Verify tooling: `java -version` (JDK 17+), `ANDROID_HOME` set, `sdkmanager --list_installed` includes `platforms;android-35` and `build-tools;35.0.0`.

## Target stack

| Concern | Choice | Version |
|---|---|---|
| Language | Kotlin | 1.9.25 |
| UI | Jetpack Compose | BOM 2024.09.00+ |
| DI | Hilt + KSP | 2.51+ / 2.0.20+ |
| Async | Coroutines + Flow | 1.8+ |
| DB | Room | 2.6+ |
| HTTP | Retrofit + OkHttp + kotlinx-serialization | 2.11 / 4.12 / 1.7 |
| Nav | Navigation Compose (typed routes) | 2.8+ |
| Image | Coil | 2.7+ |
| Lint | ktlint + detekt | latest |
| Tests | JUnit4, MockK, Turbine, Robolectric, Espresso | latest |

## SDK levels (match hisabkitab)

- `compileSdk = 35`
- `targetSdk = 34`
- `minSdk = 24`

## Module layout

```
<project>/
├── app/                            # entrypoint, navigation host
├── core/
│   ├── designsystem/               # Material 3 theme, tokens, atoms
│   ├── data/                       # repositories, mappers
│   ├── database/                   # Room
│   ├── network/                    # Retrofit + OkHttp + serializers
│   └── common/                     # shared utils, Result types
├── feature/
│   ├── <feature-a>/                # one module per feature
│   └── <feature-b>/
├── build-logic/                    # convention plugins (Gradle KTS)
├── gradle/libs.versions.toml       # version catalog
└── settings.gradle.kts
```

## Files to generate (minimum)

- `settings.gradle.kts` — declares all modules + Maven repos
- `build.gradle.kts` (root) — applies AGP, Kotlin, Hilt, KSP, detekt, ktlint plugins via aliases
- `gradle/libs.versions.toml` — single source of truth for versions
- `build-logic/convention/` — convention plugins: `android.application`, `android.library`, `android.compose`, `android.hilt`, `android.room`
- `app/build.gradle.kts`, `app/src/main/AndroidManifest.xml`, `app/src/main/java/<pkg>/MyApplication.kt`, `MainActivity.kt`, `NavGraph.kt`
- `core/designsystem/` — `Theme.kt`, `Color.kt`, `Type.kt`, `Spacing.kt`
- `core/network/` — `NetworkModule.kt` (Hilt), Retrofit + OkHttp + Json setup
- `core/database/` — `AppDatabase.kt`, `DatabaseModule.kt`
- `.editorconfig`, `detekt.yml`, `proguard-rules.pro`
- `.gitignore` with standard Android entries

## Conventions

- Package: `com.<org>.<app>` (ask user if not in spec)
- One feature per module under `feature/`; each exposes a `<Feature>NavGraph` extension
- All state is unidirectional: `UiState` sealed interface, `StateFlow` exposed by ViewModel
- Hilt entry points: `@HiltAndroidApp`, `@HiltViewModel`, `@AndroidEntryPoint`
- Tests co-located: `src/test/` for unit, `src/androidTest/` for instrumented

## Output contract

Return `{status: "ok"|"error", files_touched: [...], next_step: "run /android-new-feature <x> or android-ci-agent", error?: "..."}`.

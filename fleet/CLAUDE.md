# Android Dev Fleet — Conventions

Conventions every agent and skill in this plugin enforces.

## Two tracks, auto-detected

| Track | Detection | Stack |
|---|---|---|
| native-android | `settings.gradle{.kts}` present | Kotlin 1.9.25, Compose BOM, Hilt+KSP, Room, Retrofit, Coroutines/Flow, Material 3 |
| expo-react-native | `app.json` / `eas.json` / `"expo"` in package.json | Expo SDK 52, expo-router, Zustand, React Query, NativeWind, Supabase, expo-sqlite, jest-expo |

Greenfield (empty dir) → ask once, then proceed.

## Spec-first gate (hard rule)

No code-writing agent runs before `SPEC.md` (or `.specs/<feature>.md`) exists and is approved. The manager refuses `--skip-spec` unless an approved spec is on disk.

## Parallelism (hard rule)

When dispatching independent subagents, the manager MUST send a single message with multiple `Agent` tool calls. Sequencing independent work is a bug.

| Safe parallel | Must sequence |
|---|---|
| Multiple feature screen-gens | Scaffold → screens |
| lint ‖ unit-tests | (lint, tests) → assemble |
| Different module CI invocations | Build → emulator |
| Multiple AVD APIs in emulator | Spec → any code |

## Native standards

- SDK levels: `compileSdk = 35`, `targetSdk = 34`, `minSdk = 24`
- Kotlin 1.9.25, JDK 17+
- Gradle KTS + `gradle/libs.versions.toml` (no version literals in module files)
- Convention plugins under `build-logic/convention/`
- Module split: `:app`, `:core:{designsystem,data,database,network,common}`, `:feature:<x>`
- ktlint + detekt configured; treat warnings as warnings, errors as errors
- Compose: UDF with `UiState` sealed interface + `StateFlow` + `collectAsStateWithLifecycle`
- Hilt entry points: `@HiltAndroidApp`, `@HiltViewModel`, `@AndroidEntryPoint`
- Tests: `src/test/` (JUnit + MockK + Turbine), `src/androidTest/` (Espresso)

## Expo standards (mirroring hisabkitab)

- Monorepo with npm workspaces + `turbo.json`
- `tsconfig.base.json` strict mode; no `any` without inline justification
- `src/mobile/app/` for expo-router file-based routes: `(auth)/`, `(tabs)/`, `(modals)/`
- Zustand stores in `src/mobile/store/`
- React Query for server state; SecureStore for auth tokens
- SQLite + sync engine for offline-first
- NativeWind + Tailwind for styling
- Co-located tests: `foo.tsx` + `foo.test.tsx`, `jest-expo` preset
- All Supabase access via `@<project>/services` — never raw client in components

## File naming

- Kotlin: `PascalCase.kt` for classes, `camelCase.kt` for top-level fns
- TypeScript/React Native: `kebab-case.ts(x)`; export `PascalCase` from kebab file
- Tests: co-located, same base name + `.test.kt(s)` or `Test.kt`

## Output contract for every agent

```json
{
  "status": "ok" | "error",
  "files_touched": ["..."],
  "summary": "one line",
  "next_step": "what to do next",
  "error": null | "concise message"
}
```

The manager aggregates these into the final report.

## Idempotency

Every agent reads current state before writing. Re-running an agent on already-correct state is a no-op. The manager exploits this to retry only failed branches.

## CI must pass before "done"

A pipeline is not complete until `android-ci-agent` returns green for the relevant scope AND (when applicable) `android-emulator-agent` returns `verdict: pass`.

## Security

- Never inline keystore passwords or Supabase service keys.
- Read secrets from environment variables only.
- `.env.example` documents required vars; real `.env` / `keystore` files are gitignored.

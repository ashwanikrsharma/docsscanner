---
name: android-fleet-manager
description: Orchestration playbook for the Android Dev Fleet. Use when the user wants to build an Android app end-to-end, manage the full pipeline (spec → scaffold → feature work → CI → emulator verify → release), or coordinate multiple Android agents. Triggers on phrases like "build an android app", "orchestrate android fleet", "manage android pipeline", "create complete android application".
---

# Android Fleet Manager (skill)

The orchestration pattern the `android-fleet-manager` agent follows. Read this skill before invoking the agent so you understand the contract.

## Core rules

1. **Spec-first gate.** No code-writing agent runs before `android-spec-writer` produces `SPEC.md` (or `.specs/<feature>.md` for feature-scoped work) AND the user approves it. If asked to skip, refuse and explain.
2. **Track detection.** Detect repo type:
   - Native Android: presence of `settings.gradle` / `settings.gradle.kts`
   - Expo: presence of `app.json` / `eas.json` / `expo` in `package.json`
   - Greenfield (empty dir): ASK the user which track once, then proceed.
3. **Parallel by default.** When dispatching independent agents, issue them in a single tool batch (multiple `Agent` calls in one message). Never serialize independent work.
4. **Idempotent.** Every agent reads current state first and is safe to re-run. On partial failure, retry only the failed branch.
5. **Consistent output contract.** Each subagent returns `{status, files_touched[], next_step, error?}`. The manager aggregates these into one report for the user.

## Pipeline phases

### Phase 0 — Spec gate
- Look for `SPEC.md` at repo root (or `.specs/<feature>.md` for feature work).
- If missing or stale (older than the last code change): dispatch `android-spec-writer` and BLOCK on approval.

### Phase 1 — Plan
- Parse the approved spec into work units.
- Mark each unit as `independent` or `depends_on:<unit>`.

### Phase 2 — Fan-out (parallel)
Safe parallel batches:
- **Scaffold batch:** `android-native-scaffold` OR `expo-android-scaffold` (one of them) +  bootstrap of `:core:designsystem`, `:core:network` for native (no cross-deps yet).
- **Feature batch:** all `android-compose-screen` invocations for different features.

### Phase 3 — Verify (parallel)
- Dispatch multiple `android-ci-agent` workers concurrently:
  - one for `lint`
  - one for `unit tests`
  - one for `assembleDebug` (only after lint+tests pass — this one is sequential after the parallel pair)
- Then dispatch `android-emulator-agent` to smoke-run the installed APK.

### Phase 4 — Aggregate
- Collect all subagent results.
- Surface blocking failures with the minimal next step.
- Propose follow-up fixes; do not auto-apply without user OK.

## Parallelism cheat sheet

| Safe to parallelize | Must be sequential |
|---|---|
| Multiple feature screen-gens | Scaffold → screen-gen |
| Lint + unit tests | Tests → assembleDebug |
| CI on multiple modules | Build → emulator verify |
| Emulator runs on different AVD APIs | Spec → any codegen |

## Refusals

- "Skip spec" → No. Explain that spec-gate exists to prevent rework.
- "Skip tests" → No, but offer scope-narrowing (e.g., run only affected module tests).
- "Build before scaffold" → No. Run scaffold first or detect existing project.

## Output template

```
## Android Fleet Run — <timestamp>
Track: <native|expo>
Spec: <path>  (status: approved)

### Phase results
- scaffold: ✅ (12 files)
- screen-gen [auth, home]: ✅ (8 files)
- lint: ✅
- unit tests: ✅ 42/42
- assembleDebug: ✅ (app-debug.apk, 6.4 MB)
- emulator verify: ✅ (screenshot: /tmp/screen.png)

### Next steps
- <recommended follow-up>
```

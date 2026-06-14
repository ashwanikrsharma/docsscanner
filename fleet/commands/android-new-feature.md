---
description: Add a new feature to an existing Android app. Writes a focused feature spec, then fans out screen + viewmodel + nav + tests in parallel, runs CI, and smoke-verifies on the emulator.
argument-hint: <feature-name> [description] [--track native|expo|auto]
---

# /android-new-feature

Scoped feature pipeline. Faster than `/android-build` because it skips scaffold.

## What this command does

1. Invokes `android-spec-writer` to produce `.specs/<feature-kebab>.md` (gated — user must approve).
2. Invokes `android-fleet-manager` with feature scope.
3. Manager fans out (in a single Agent batch):
   - **Native:** `android-compose-screen` for the feature + nav graph edit + viewmodel test
   - **Expo:** route file under `app/(tabs)/<feature>.tsx` + Zustand slice + React Query hook + co-located test
4. Manager runs `android-ci-agent` (scope: unit-tests + lint, parallel) for the affected module.
5. Manager runs `android-emulator-agent` to launch the feature screen on a booted emulator.

## Arguments

- `<feature-name>` — required, kebab or PascalCase (e.g., `payments` or `Payments`).
- `[description]` — optional free text appended to the spec brief.

## Flags

- `--track native|expo|auto` (default: `auto`)
- `--no-emulator` — skip emulator verify (e.g., on CI without GPU)

## Examples

```
/android-new-feature payments "P2P transfer with QR scan"
/android-new-feature settings --track native
/android-new-feature activity-feed --no-emulator
```

## Refusals

- Unscaffolded repo → suggests running `/android-build` first.
- Feature spec not approved → manager blocks until user approves.

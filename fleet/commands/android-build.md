---
description: Run the full Android Dev Fleet pipeline end-to-end — spec gate → scaffold → features → CI (parallel) → emulator verify. Invokes the android-fleet-manager agent. Use for greenfield apps or large multi-feature builds.
argument-hint: [goal-or-spec-path] [--track native|expo|auto] [--skip-spec] [--parallel N]
---

# /android-build

Kick off the orchestrated Android build pipeline.

## What this command does

1. Resolves the goal: either a free-text goal (e.g., "expense tracker like hisabkitab") OR a path to an existing `SPEC.md`.
2. Invokes the **android-fleet-manager** agent with the resolved goal + flags.
3. The manager handles spec-gate, scaffold, parallel feature fan-out, parallel CI, and emulator verification.

## Flags

- `--track native|expo|auto` (default: `auto`) — force a track or let the manager detect.
- `--skip-spec` — only valid when an approved `SPEC.md` already exists. Manager refuses otherwise.
- `--parallel N` (default: `4`) — cap concurrent subagent invocations.

## Examples

```
/android-build "expense tracker like hisabkitab"
/android-build SPEC.md --track native
/android-build --skip-spec --parallel 6
```

## Behavior

The agent will:
- Phase 0: ensure spec exists (else run `android-spec-writer` and BLOCK on approval).
- Phase 1-2: scaffold once track is chosen.
- Phase 3: fan out feature work in a single tool batch (multiple Agent calls in one message).
- Phase 4: run `android-ci-agent` in parallel for lint + unit tests; then assemble; then `android-emulator-agent`.
- Phase 5: print aggregated report; write `.fleet/last-run.md`.

## Refusals

- `--skip-spec` without an approved `SPEC.md` → refused.
- Mid-pipeline destructive failures → manager stops, asks user how to proceed.

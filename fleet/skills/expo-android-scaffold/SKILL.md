---
name: expo-android-scaffold
description: Scaffold a React Native Expo (SDK 52) Android app using expo-router, Zustand, React Query, NativeWind, Supabase, and SQLite, modeled after the hisabkitab monorepo at /Users/asharma52/github/hisabkitab. Use when the user asks to create, bootstrap, or initialize an Expo / React Native project targeting Android. Not for native Kotlin (use android-native-scaffold for that).
---

# Expo Android Scaffold

Bootstrap an Expo SDK 52 React Native app that targets Android, mirroring the structure of `/Users/asharma52/github/hisabkitab`.

## Pre-flight

- Require `SPEC.md` to be approved.
- Verify tooling: `node --version` (≥20), `npm --version` (≥10), JDK 17+ for prebuild, Android SDK installed.

## Target stack (matches hisabkitab)

| Concern | Choice | Version |
|---|---|---|
| Framework | Expo | ~52.0.0 |
| Router | expo-router | ~4.0.0 |
| Runtime | React Native | 0.76.9 |
| State | Zustand | ^4.5.2 |
| Data | @tanstack/react-query | ^5.40.0 |
| Styling | NativeWind + Tailwind | 4.0.1 / 3.4.3 |
| Local DB | expo-sqlite | ~15.1.4 |
| Backend | @supabase/supabase-js | ^2.43.5 |
| Auth storage | expo-secure-store | ~14.0.0 |
| Tests | jest, jest-expo, @testing-library/react-native | 29 / 52 / 13 |

## Android SDK levels (match hisabkitab)

- `compileSdk = 35`, `targetSdk = 34`, `minSdk = 24`, Kotlin 1.9.25, Hermes on.

## Monorepo layout

```
<project>/
├── package.json                 # npm workspaces
├── tsconfig.base.json           # strict: true
├── turbo.json
├── src/
│   ├── mobile/                  # Expo app
│   │   ├── app/
│   │   │   ├── _layout.tsx
│   │   │   ├── (auth)/
│   │   │   ├── (tabs)/
│   │   │   └── (modals)/
│   │   ├── components/          # co-located *.test.tsx
│   │   ├── hooks/
│   │   ├── lib/                 # local-db, sync-engine, theme
│   │   ├── store/               # Zustand: auth, theme
│   │   ├── app.config.ts
│   │   ├── metro.config.js
│   │   ├── babel.config.js
│   │   ├── jest.config.js
│   │   └── tsconfig.json
│   ├── services/                # Supabase queries, types
│   └── shared/                  # shared types/utils
└── .env.example
```

## Files to generate (minimum)

- Root `package.json` with `workspaces: ["src/*"]`
- `tsconfig.base.json` (strict mode), `turbo.json`
- `src/mobile/app.config.ts` (Expo plugins, Android permissions, package name)
- `src/mobile/app/_layout.tsx` — QueryClient, ThemeProvider, sync init
- `src/mobile/app/(tabs)/_layout.tsx`, sample home screen + co-located test
- `src/mobile/store/auth.ts` — Zustand + Supabase + SecureStore adapter
- `src/mobile/store/theme.ts` — light/dark with persistence
- `src/mobile/lib/local-db.ts` + `local-db-schema.ts` — SQLite skeleton
- `src/mobile/lib/sync-engine.ts` — pull/push stub
- `src/mobile/lib/api-client.ts`, `secure-store-adapter.ts`, `network-status.ts`
- `src/mobile/tailwind.config.js`, NativeWind setup
- `src/mobile/jest.config.js` (`preset: 'jest-expo'`)
- `src/services/package.json`, `src/services/src/{client,types,index}.ts`
- `.env.example` with `EXPO_PUBLIC_SUPABASE_URL`, `EXPO_PUBLIC_SUPABASE_ANON_KEY`
- `.gitignore` (Expo + Node defaults)

## Conventions

- Files: `kebab-case.ts(x)`; components: `PascalCase` export from kebab file
- Co-locate tests: `foo.tsx` + `foo.test.tsx`
- All Supabase access via `@<project>/services` — never raw client in components
- Bearer token from SecureStore on mobile; cookie on web (if web added later)
- TypeScript strict; no `any` without comment

## Reference

When generating, treat `/Users/asharma52/github/hisabkitab/src/mobile/` as the canonical example. Read specific files there for patterns when unsure (e.g., `store/auth.ts`, `lib/local-db.ts`, `lib/sync-engine.ts`).

## Output contract

Return `{status, files_touched, next_step, error?}`.

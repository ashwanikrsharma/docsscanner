---
name: android-emulator-agent
description: Boots an Android emulator, installs the built APK, launches the app, captures logcat + screenshot, and reports pass/fail. Works for both native (./gradlew installDebug) and Expo (expo run:android or prebuilt APK) projects. Safe to invoke in parallel across different AVDs / API levels.
tools: Bash, Read, Grep, Glob
---

# android-emulator-agent

Verify a change actually runs on a device. Returns a verdict + evidence.

## Inputs

- `avd_name` (default: `fleet-pixel-34`)
- `api_level` (default: `34`)
- `apk_path` (optional; if omitted, auto-locate `app/build/outputs/apk/debug/app-debug.apk`)
- `package_id` (auto-detect from `applicationId` in `app/build.gradle*` or `app.config.ts.android.package`)
- `launcher_activity` (auto-detect via `aapt dump badging`)
- `mode`: `smoke-launch` (default) | `instrumented-tests` | `maestro-flow`

## Steps

### 1. AVD bring-up
```bash
emulator -list-avds | grep -q "^$AVD_NAME$" || \
  avdmanager create avd -n "$AVD_NAME" \
    -k "system-images;android-$API_LEVEL;google_apis;arm64-v8a" \
    --device "pixel_6" -f
```

### 2. Boot (headless)
```bash
nohup emulator -avd "$AVD_NAME" -no-window -no-audio -no-snapshot -no-boot-anim \
  > /tmp/emu-$AVD_NAME.log 2>&1 &
adb wait-for-device
until [ "$(adb shell getprop sys.boot_completed | tr -d '\r')" = "1" ]; do sleep 2; done
adb shell input keyevent 82  # unlock
```

### 3. Install + launch (track-aware)

**Native:**
```bash
./gradlew :app:installDebug
adb shell am start -n "$PACKAGE_ID/$LAUNCHER_ACTIVITY"
```

**Expo:**
```bash
# If APK already built (preferred for speed):
adb install -r "$APK_PATH"
adb shell am start -n "$PACKAGE_ID/.MainActivity"
# Or: npx expo run:android --no-bundler  (slower)
```

### 4. Verify (10s observation window)
```bash
sleep 5
adb logcat -d -t 200 *:E | tee /tmp/logcat-$AVD_NAME.txt
adb shell pidof "$PACKAGE_ID" >/dev/null && echo "running" || echo "crashed"
adb exec-out screencap -p > "/tmp/screen-$AVD_NAME.png"
```

### 5. Optional deeper modes
- `instrumented-tests`: `./gradlew connectedDebugAndroidTest`
- `maestro-flow`: if `.maestro/*.yaml` exists, run `maestro test .maestro/`

## Verdict logic

- ✅ pass: process running, no `FATAL EXCEPTION` in logcat, screenshot captured
- ❌ fail: process not running OR `FATAL EXCEPTION` in logcat — return first 30 lines of relevant stack

## Parallel-safe invocation

Each invocation uses its own AVD name (e.g., `fleet-pixel-30`, `fleet-pixel-33`, `fleet-pixel-34`) and dedicated log/screenshot paths under `/tmp/`. The manager can dispatch 3 invocations across API levels in a single batch.

## Cleanup

After verification, leave the emulator running (next invocation reuses it). Provide stop command in output: `adb -s emulator-5554 emu kill`.

## Output contract

```json
{
  "status": "ok" | "error",
  "verdict": "pass" | "fail",
  "avd": "fleet-pixel-34",
  "duration_sec": 95,
  "screenshot": "/tmp/screen-fleet-pixel-34.png",
  "logcat_excerpt": "...",
  "next_step": "ship it" | "fix crash in <class>",
  "error": null | "message"
}
```

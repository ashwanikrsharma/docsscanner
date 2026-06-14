# PocketScan

**A secure, offline-first Android document scanner. Free to build. Free to run.**

PocketScan lets you scan paper documents with the Google ML Kit on-device document scanner, save them as PDFs in app-private storage, view them in a recent list, and securely share or rename/delete them. No backend. No cloud processing. No ads. No analytics. No subscriptions.

---

## Core features

- 📄 **Scan to PDF** — Powered by Google ML Kit Document Scanner (on-device, no Internet permission).
- 🗂️ **Recent scans** — Browse, open, rename, share, and delete.
- 🔐 **On-device only** — All documents stored in app-private storage. Nothing leaves the device unless you explicitly tap Share.
- 🎨 **Material 3 UI** — Light + dark theme, dynamic color on Android 12+, accessible touch targets, modern Compose UI.
- 🛡️ **Privacy by design** — No analytics, no tracking, no Internet permission, FLAG_SECURE on sensitive screens, FileProvider for sharing.

## Screenshots

_Placeholder — add screenshots from `app-debug.apk` running on a device or emulator._

| Home | Viewer | Privacy |
|---|---|---|
| _empty + populated states_ | _details + actions_ | _disclosure copy_ |

## Tech stack

- **Language:** Kotlin 2.0.20
- **UI:** Jetpack Compose, Material 3, Compose BOM 2024.09.02
- **Architecture:** MVVM with `StateFlow` UI state; small manual DI container (no Hilt — keeps build cost low)
- **Persistence:** Room 2.6.1 (KSP)
- **Async:** Kotlin Coroutines 1.8.1
- **Scanning:** Google ML Kit Document Scanner (`play-services-mlkit-document-scanner:16.0.0-beta1`)
- **Navigation:** Navigation Compose 2.8.1
- **PDF preview/thumbnail:** AndroidX `PdfRenderer` (built into the platform)
- **Build:** AGP 8.5.2, Gradle KTS, version catalog (`gradle/libs.versions.toml`)
- **SDKs:** `compileSdk=35`, `targetSdk=34`, `minSdk=24`

## Setup steps

### Prerequisites
- JDK 17+
- Android SDK with `platforms;android-35` and `build-tools;35.0.0`
- `ANDROID_HOME` environment variable set

### Clone & open
```bash
git clone <your-fork-or-zip> pocketscan
cd pocketscan
```

Open in Android Studio (Iguana / Koala or newer) — Gradle sync will pull dependencies from `google()` and `mavenCentral()`.

## How to run locally

From Android Studio: pick a Pixel 6 emulator running Android 13 or 14 with **Google Play services** installed (system image `google_apis` or `google_apis_playstore`) and click ▶︎ Run.

From the command line:
```bash
./gradlew :app:installDebug
adb shell am start -n com.pocketscan.app/.MainActivity
```

> ⚠️ ML Kit Document Scanner requires Google Play services. Emulator images without Google Play services will not be able to launch the scanner UI.

## How to build APK

```bash
# Debug APK (sideloadable):
./gradlew :app:assembleDebug
# Result: app/build/outputs/apk/debug/app-debug.apk

# Release APK (minified, requires signing config you provide via env):
./gradlew :app:assembleRelease
```

## UI/UX design principles used

- **Material Design 3** — color scheme, typography scale, surface elevation, FAB pattern
- **Dynamic color** — picked up from the user's wallpaper on Android 12+
- **Light & dark themes** — system-driven; previews supported via Compose
- **Accessibility** — 48dp minimum touch targets, content descriptions on icon buttons, semantic roles via Material components
- **Clear hierarchy** — single primary action (Scan), recent list with cards, secondary actions accessible from viewer screen
- **Calm empty/loading/error states** — Snackbars for transient feedback, AlertDialog for destructive confirmation
- **Edge-to-edge** — `enableEdgeToEdge()` for modern Android shape

## Privacy note

**Your scans stay on your device.** This app does not upload, sell, track, or analyze your documents. The in-app Privacy screen (accessible from the home toolbar) restates this in user-friendly language.

## Security design

- **No INTERNET permission.** The app's `AndroidManifest.xml` declares no Internet permission. The ML Kit scanner module is downloaded at the OS/Play-services level, not by this app.
- **App-private storage.** Scans live in `context.filesDir/documents/` — not accessible to other apps.
- **FileProvider for sharing.** Raw paths are never exposed. `FLAG_GRANT_READ_URI_PERMISSION` grants temporary read access only to the app the user explicitly picks.
- **FLAG_SECURE on `MainActivity`.** Blocks screenshots and the Recents-screen thumbnail of any document content.
- **Path safety.** File names are sanitized to strip path separators, reserved characters, and `..` sequences. All written files are validated to live under `filesDir` (no escape).
- **Backup opt-out.** `data_extraction_rules.xml` excludes all domains from cloud backup and device-to-device transfer.
- **No exported components except `MainActivity`.** `MainActivity` is exported because it must be the launcher. The FileProvider is `exported="false"` (required by FileProvider).
- **Immutable PendingIntent** policy: this app does not currently use PendingIntent. If added later, use `FLAG_IMMUTABLE`.
- **No SharedPreferences for sensitive data.** Metadata lives in Room; no secrets are persisted.
- **Temporary scanner Uris not relied upon.** The scanner's output Uri is immediately copied into app storage; if metadata insert fails afterward, the copied file is removed.

## Storage design

- PDFs: `filesDir/documents/<uuid>.pdf`
- Thumbnails (JPEG page 0 rendering): `filesDir/thumbnails/<uuid>.jpg`
- Metadata: Room database `pocketscan.db`, table `documents`
- File and metadata operations are sequenced so a failure in either rolls back the other where possible.

## Permissions used and why

- **None declared.** No `INTERNET`, no `CAMERA`, no `READ/WRITE_EXTERNAL_STORAGE`.
- The ML Kit Document Scanner launches its own UI that handles camera permission itself at the system level — this app does not need to request camera permission.

## Third-party libraries and licenses

| Library | License |
|---|---|
| AndroidX Core, Lifecycle, Activity Compose, Compose UI, Material 3, Navigation Compose, Room | Apache 2.0 |
| Kotlin standard library, Coroutines | Apache 2.0 |
| Google ML Kit Document Scanner (`play-services-mlkit-document-scanner`) | Apache 2.0 (Google Play Services Client SDK terms apply) |
| JUnit 4 | EPL 1.0 |
| Google Truth (test only) | Apache 2.0 |

All runtime dependencies are free for personal and commercial use under permissive licenses.

## Cost statement

- **Build cost:** $0. All libraries are free.
- **Runtime cost:** $0. No backend, no cloud APIs, no analytics, no crash reporting, no ads, no per-scan fee.
- **Developer cost:** $0 for local development and sideloading. The optional Google Play Developer account is a one-time fee, separate from this app's runtime/build cost.

## Known limitations

- ML Kit Document Scanner requires **Google Play services**. Devices without Play services cannot scan.
- In-app PDF preview is intentionally not implemented; the app uses an external PDF viewer intent to keep the build dependency-free.
- No cloud backup or multi-device sync (by design).
- No OCR. ML Kit Text Recognition could be added later; it is also free and on-device but adds binary size and processing surface, so it's deferred.

## Future enhancements

- Optional on-device OCR using ML Kit Text Recognition (still free, still offline).
- In-app PDF preview (would require a free renderer; `PdfRenderer` is usable but limited).
- Tags / folders for organizing scans.
- Quick share to a default app from the home list.
- Light/dark/dynamic-color preview composables.

## Project layout

```
app/src/main/java/com/pocketscan/app/
├── PocketScanApplication.kt
├── MainActivity.kt
├── di/AppContainer.kt
├── data/
│   ├── DocumentEntity.kt
│   ├── DocumentDao.kt
│   ├── AppDatabase.kt
│   ├── DocumentFileManager.kt
│   └── DocumentRepository.kt
├── domain/Document.kt
├── scanner/DocumentScannerManager.kt
└── ui/
    ├── navigation/PocketScanNavHost.kt
    ├── home/{HomeScreen,HomeViewModel,DocumentCard}.kt
    ├── viewer/{ViewerScreen,ViewerViewModel}.kt
    ├── privacy/PrivacyScreen.kt
    ├── components/{RenameDialog,DeleteDialog,Format}.kt
    └── theme/{Theme,Color,Type}.kt
```

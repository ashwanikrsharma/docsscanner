# PocketScan — Technical Spec

**Track:** native-android
**Status:** approved (auto-approved per user requirement document)
**Author:** ashwanikumar_sharma@intuit.com
**Date:** 2026-06-14

## 1. Goals & non-goals
- **Goals:**
  - Offline-first Android document scanner using only free libraries
  - Scan → PDF saved in app-private storage, surfaced as a recent list
  - Rename, delete, share (via FileProvider), view details
  - Polished Material 3 UI, light/dark, dynamic color on 12+
  - Zero runtime cost, zero tracking, zero backend
- **Non-goals:** cloud backup, multi-device sync, in-app PDF rendering, OCR (deferred), accounts/login, ads, analytics

## 2. User stories
- As a user, I tap "Scan" and the ML Kit scanner captures my document and saves a PDF.
- As a user, I see my recent scans on the home screen with name, date, page count, size.
- As a user, I can rename, delete, or share any scan.
- As a user, I can tap a scan to view details and open it in an external PDF viewer.
- As a user, I can read a Privacy screen that confirms nothing leaves my device.

## 3. Architecture
- **Track:** native Android (Kotlin + Jetpack Compose)
- **Pattern:** MVVM, single-module (overengineered multi-module split skipped per "avoid overengineering")
- **Modules:** single `app` module organized by package: `data/`, `domain/`, `scanner/`, `ui/{navigation,home,viewer,privacy,components,theme}`
- **Key libs:**
  - Kotlin 1.9.25, AGP 8.5.x, JDK 17
  - Compose BOM 2024.09.00, Material 3
  - ML Kit Document Scanner `play-services-mlkit-document-scanner:16.0.0-beta1`
  - Room 2.6.1 (KSP)
  - Lifecycle + ViewModel Compose 2.8.x
  - Navigation Compose 2.8.x
  - Coroutines 1.8.x
  - No Hilt — keep DI manual via an `AppContainer` to avoid extra KSP processors and stay minimal
- **SDK:** `compileSdk=35`, `targetSdk=34`, `minSdk=24` (24 chosen over 23 because ML Kit scanner requires 21+, Compose recommends 24+; covers >97% devices)

## 4. Data model
- **DocumentEntity (Room):** `id: Long (PK, autoGen)`, `name: String`, `pdfPath: String` (relative to app filesDir), `createdAt: Long (epoch ms)`, `updatedAt: Long`, `pageCount: Int`, `fileSizeBytes: Long`, `thumbnailPath: String?`
- **Storage:** `filesDir/documents/<uuid>.pdf` for PDFs; `filesDir/thumbnails/<uuid>.jpg` for thumbnails
- **Migrations:** schema v1; no destructive fallback configured (we'll add migrations only when schema changes)

## 5. API contracts
- None. No INTERNET permission. No remote APIs.
- ML Kit Document Scanner downloads its scanner module from Play Services on first use — this is OS-level, not an app network permission.

## 6. Navigation map
- `home` → list of scans, FAB "Scan", overflow → Privacy
- `viewer/{id}` → details + actions (share, rename, delete, open externally)
- `privacy` → static text screen
- Back button returns to previous; deletion from viewer pops to home.

## 7. State flows
- `HomeViewModel`: `StateFlow<HomeUiState>` with `Loading | Empty | Success(items) | Error`
- `ViewerViewModel`: `StateFlow<ViewerUiState>` with `Loading | Loaded(doc) | Deleted | Error`
- Events surfaced via one-shot `SharedFlow<UiEvent>` for snackbars/toasts

## 8. Telemetry
- **None.** Hard requirement.

## 9. Testing strategy
- **Unit:** `DocumentFileManager` (sanitize, copy, delete), `DocumentRepository` (CRUD orchestration). JUnit4 + Truth + Coroutines test.
- **Instrumented:** skipped for initial build (emulator agent smoke-launches the app instead).
- **Emulator smoke:** boot AVD → install → launch → assert process alive, no FATAL EXCEPTION.

## 10. Open questions (all defaulted)
- Thumbnail generation: default = none initially (ML Kit only returns PDF Uri; PDF→bitmap requires `PdfRenderer` which is fine and free — we'll use it).
- FLAG_SECURE: default = applied on `MainActivity` to cover preview screens.
- Dynamic color on 12+: default = enabled.
- OCR: default = deferred (listed as future enhancement).

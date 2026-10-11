# System Architecture

## 1. Architecture principles

- Local-first: Room/SQLite is the system of record in MVP.
- Explicit boundaries: Android listener, processing, persistence, domain rules, and UI are separate.
- Best-effort capture: never claim complete coverage.
- Minimize collected fields and avoid storing raw `StatusBarNotification` or Android framework objects.
- Testable logic: normalization, filtering, retention and search rules should be unit-testable.

## 2. Logical architecture

```text
Android apps
    ↓ post/update
Android Notification Manager
    ↓ system listener event (only after user enables access)
VaultNotificationListener (thin adapter)
    ↓ normalized candidate event
CaptureProcessor / PrivacyFilter
    ↓ accepted record
NotificationRepository
    ↓
Room Database / SQLite
    ↓ Flow queries
ViewModel
    ↓ UI state
Jetpack Compose screens
```

Retention:
- WorkManager schedules deferrable cleanup.
- Cleanup calls repository/domain retention logic.
- Do not use WorkManager for real-time capture.

## 3. Recommended package layout

```text
com.example.notificationvault/
├── MainActivity.kt
├── app/
│   ├── NotificationVaultApp.kt
│   └── navigation/
├── platform/
│   ├── NotificationAccessChecker.kt
│   └── listener/VaultNotificationListener.kt
├── data/
│   ├── local/VaultDatabase.kt
│   ├── local/NotificationDao.kt
│   ├── local/NotificationEntity.kt
│   ├── local/Converters.kt
│   └── repository/NotificationRepositoryImpl.kt
├── domain/
│   ├── model/ArchivedNotification.kt
│   ├── capture/CaptureProcessor.kt
│   ├── capture/NotificationNormalizer.kt
│   ├── policy/PrivacyPolicy.kt
│   ├── policy/RetentionPolicy.kt
│   └── repository/NotificationRepository.kt
├── ui/
│   ├── onboarding/
│   ├── inbox/
│   ├── search/
│   ├── bookmarks/
│   ├── details/
│   └── settings/
├── worker/RetentionCleanupWorker.kt
└── util/Clock.kt
```

Package names may change before implementation, but update this document and all examples together.

## 4. Listener responsibilities

`VaultNotificationListener` should:
- Handle `onNotificationPosted` and, where relevant, update callbacks according to the chosen Android API contract.
- Ignore the app's own notifications if the app later creates any, to avoid self-capture loops.
- Extract only allowed fields available from the notification extras.
- Respect package-level capture settings.
- Send a small normalized event to the processing/repository layer.
- Avoid long database work on the callback path and never block the main thread.
- Handle nullable/missing extras.
- Never treat `onNotificationRemoved` as an instruction to delete the archive.

Implementation note: confirm the callback/threading contract for the selected compile SDK. Use a lifecycle-safe execution strategy; do not introduce `GlobalScope`. If using a service-owned coroutine scope, cancel it appropriately and make repository operations safe if the service disconnects.

## 5. Capture semantics

- A record is created or updated when an eligible posted/updated event is received.
- `eventTime` is taken from the platform event when available; `capturedAt` is taken from an injected clock at processing time.
- Notification keys are transient identifiers, not guaranteed permanent IDs.
- Updates should not create unlimited duplicate records. Define a stable deduplication strategy and test it.
- The listener receives only what Android and the source app expose. Do not attempt to reconstruct hidden content.

## 6. UI state

Use ViewModels with `StateFlow` or lifecycle-aware Flow collection. UI state should distinguish:
- Loading
- Permission not granted
- Empty archive
- Results
- No search results
- Database/load error

Do not infer permission state from a previous session; check the current system state when the app resumes.

## 7. Dependency policy

Select versions that are mutually compatible and current at implementation time. Pin versions through a version catalog (`gradle/libs.versions.toml`) where practical. Verify each dependency against official release notes. No invented or unverified version strings.

Core dependency categories:
- Android Gradle Plugin + Kotlin plugin
- Compose BOM + Material 3 + UI tooling
- Lifecycle ViewModel Compose
- Room runtime/compiler (KSP where compatible)
- Kotlin Coroutines
- WorkManager (only if used)
- JUnit, AndroidX test, Compose UI test, Room testing

## 8. Permissions and manifest

- Declare the service with the platform-required service permission and intent filter for notification listener binding.
- Do not request `INTERNET`, contacts, SMS, call log, location, accessibility service, or broad storage access for the MVP.
- The user must enable notification access through Android settings; this is not an ordinary runtime permission dialog.
- Confirm manifest declaration against current Android documentation before coding.

## 9. Error handling

- Database write failure: record a sanitized internal error state/metric without payload text; do not crash the app.
- Missing app label: use a safe fallback such as package name.
- Missing content: save allowed metadata and show “No preview available.”
- Permission loss: reflect it on next foreground/resume check.
- Process death: rely on durable database state; do not assume in-memory work survived.
- Migration failure: test and define a safe recovery path; never silently delete user data to make migration pass.

## 10. Future extension boundaries

Cloud backup, sync, AI summaries, analytics or accounts require:
1. updated PRD;
2. new ADR;
3. updated threat model and data-flow diagram;
4. privacy notice/disclosure review;
5. explicit opt-in design;
6. tests for deletion, export, consent and failure modes.

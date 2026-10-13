# Test Plan and Quality Strategy

## 1. Test layers

1. Unit tests: normalizer, filtering, deduplication, retention policy, search query construction.
2. DAO/database tests: insert, update, delete, filters, indexes, migrations.
3. ViewModel tests: UI states, query/filter combination, bookmarks.
4. Instrumented tests: Room on device/emulator, Compose navigation, settings state.
5. Manual system tests: notification listener access and real app notifications.
6. Security checks: payload logging, manifest permissions, export handling.
7. Release checks: signed artifact, install/upgrade, smoke test.

## 2. Test fixtures

Use synthetic notifications with obvious sentinel strings such as `PRIVATE_SENTINEL_DO_NOT_LOG_8472`. Never use real private messages or OTPs as fixtures.

## 3. Required test cases

| ID | Scenario | Expected result |
|---|---|---|
| TC-001 | Access disabled on first launch | Explainer shown; no false active state |
| TC-002 | Access enabled and test notification posted | Eligible fields saved locally |
| TC-003 | System shade notification dismissed | Archive row remains |
| TC-004 | Notification body missing | No crash; body shown as unavailable/omitted |
| TC-005 | Title/body redacted by source/OS | No fabricated text |
| TC-006 | Notification updated | Update/dedupe policy behaves as documented |
| TC-007 | Two legitimate identical alerts | Both preserved unless exact update relationship is established |
| TC-008 | Same key updated rapidly | No unbounded duplicate rows |
| TC-009 | Per-app capture disabled | New event from package not saved |
| TC-010 | Global capture disabled | New events not saved |
| TC-011 | Permission revoked while app backgrounded | On resume, status reflects off |
| TC-012 | Permission re-enabled | Future events captured; gap not backfilled |
| TC-013 | Bookmark record then retention cleanup | Bookmarked record retained |
| TC-014 | Expired non-bookmarked record | Deleted after cleanup runs |
| TC-015 | Keep-until-deleted selected | Automatic cleanup does not delete records |
| TC-016 | Search case variation | Matching result returned |
| TC-017 | Search contains SQL wildcard characters | Input treated safely; no query injection |
| TC-018 | App + date + text filters combined | Results satisfy all active filters |
| TC-019 | Clear-all confirmed | All rows removed |
| TC-020 | Clear-all cancelled | No rows removed |
| TC-021 | Process killed/reopened | Persisted rows still display |
| TC-022 | Device reboot | Permission/capture state is rechecked; no false backfill claim |
| TC-023 | Database migration | Data retained and schema updated |
| TC-024 | Database write failure simulation | App does not crash; sanitized error handling |
| TC-025 | Sentinel text processed | Sentinel absent from Logcat and crash logs |
| TC-026 | Manifest audit | No unapproved internet, SMS, contacts, call log, location, accessibility permissions |
| TC-027 | Very long title/body | Field caps/truncation policy works |
| TC-028 | Burst of notifications | UI remains responsive; no main-thread blocking |
| TC-029 | Missing app label | Safe package-name fallback |
| TC-030 | Source app launch unavailable | Graceful message; no crash |
| TC-031 | Retention boundary timestamp | Cutoff inclusivity matches documented rule |
| TC-032 | Export (if enabled) | Explicit user action, warning, user-chosen destination, no upload |

### 3.1 Automated Security & Privacy Test Coverage (Task 4.4)

#### TC-025: Notification Payload Logging Audit
- **Test Class:** `com.example.notificationvault.security.PrivacyLoggingAuditTest`
- **Methodology:** Exercises full capture pipeline, exclusion evaluation, and SQLite database write failure handling with identifiable sentinel strings (`PRIVATE_SENTINEL_DO_NOT_LOG_8472`, `Confidential OTP code 491028 for account verification`, `SECRET_EXTRA_DATA_VALUE_99182`).
- **Verifications:**
  - `capturePipeline_processesSentinelNotification_withoutEmittingPayloadToLogs`: Ingests sentinel notification, confirms persistence into local Room database on `Dispatchers.IO`, and verifies 0% presence across all Robolectric Logcat priorities (V, D, I, W, E, WTF), stdout, and stderr.
  - `capturePipeline_excludedPackage_doesNotEmitPayloadToLogs`: Validates package exclusion decisions do not log notification payloads.
  - `capturePipeline_databaseFailure_doesNotEmitPayloadToLogs`: Injects simulated `SQLiteDiskIOException` during Room insert and asserts sanitized exception handling leaves no payload traces in Logcat or console streams.
- **Status:** Automated, PASS (3/3 tests passing).

#### TC-026: Manifest Permissions & Dependency Security Audit
- **Test Class:** `com.example.notificationvault.security.ManifestAndDependencyAuditTest`
- **Methodology:** Audits source manifest (`app/src/main/AndroidManifest.xml`) and effective merged manifests (`debug` and `release`), as well as version catalogs and runtime classpaths.
- **Verifications:**
  - `manifestAudit_prohibitsUnapprovedPermissionsInAllManifests`: Confirms zero forbidden permissions (`INTERNET`, `ACCESS_NETWORK_STATE`, SMS, contacts, location, mic, camera, accessibility) in source and merged manifests. Verifies `ACCESS_NETWORK_STATE` introduced transitively by WorkManager is stripped via `tools:node="remove"`.
  - `manifestAudit_allowBackupIsExplicitlyDisabled`: Asserts `android:allowBackup="false"` in effective application configurations.
  - `manifestAudit_notificationListenerService_isProperlyProtected`: Asserts `VaultNotificationListener` is exported (`true`) and strictly protected by `android.permission.BIND_NOTIFICATION_LISTENER_SERVICE`.
  - `manifestAudit_exportedComponents_areStrictlyControlled`: Verifies only `MainActivity` is an exported app activity; all exported library components are protected by system permissions (`BIND_JOB_SERVICE`, `DUMP`); all content providers are non-exported (`exported="false"`).
  - `dependencyAudit_noUnauthorizedTelemetryOrAnalyticsDeclared`: Audits Gradle version catalog (`libs.versions.toml`), `app/build.gradle.kts`, and resolved runtime classpaths (`debugRuntimeClasspath` and `releaseRuntimeClasspath`) to guarantee zero telemetry, analytics, ad networks, or crash-reporting SDKs.
- **Status:** Automated, PASS (5/5 tests passing).

## 4. Commands

Run from repository root, adapting task names to the actual Gradle project:
- `./gradlew test`
- `./gradlew lint`
- `./gradlew assembleDebug`
- `./gradlew connectedCheck` (requires a configured emulator/device and tests)
- `./gradlew assembleRelease` (release configuration required)

On Windows PowerShell, use `.\gradlew.bat <task>`.

## 5. Test data privacy

- Use synthetic data.
- Do not attach user notification exports to bug reports.
- When sharing logs, inspect and sanitize them first.
- CI artifacts must not contain local databases or test notification payloads.

## 6. Exit criteria

- All P0 cases pass or have an explicitly approved exception.
- No critical/high security finding remains.
- Build/lint/unit tests pass on clean checkout.
- Physical-device test evidence is recorded.
- Known limitations are documented.

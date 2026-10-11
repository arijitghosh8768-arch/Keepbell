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

# Product Requirements Document (PRD)

**Product:** Notification Vault  
**Version:** 1.0  
**Status:** Baseline for MVP implementation

## 1. Problem

People dismiss notification panels in bulk and can lose access to useful notification snapshots. Existing Android notification history varies by device/version and may have limited retention or discovery. Users need a searchable, user-controlled archive of eligible notifications that were received while the app had access.

## 2. Product goal

Provide a dependable, transparent, local archive of notifications that Android makes available to the listener after the user enables notification access.

## 3. Non-goals

- Recovering notifications from before installation or before access was granted.
- Recovering deleted messages from the source app or bypassing end-to-end encryption.
- Guaranteeing full message bodies, sender identity, attachments, or images.
- Replacing the source app's message store or notification panel.
- iOS support in MVP.
- Cloud sync, social features, accounts, subscriptions, ads, or AI summaries in MVP.
- Reading SMS/call logs or other data outside the notification listener's scope.

## 4. Target users

- People who receive many notifications and need to retrieve prior alerts.
- Students who need to revisit class, assignment, and schedule notifications.
- Users who want an archive but require clear privacy controls.

## 5. Core user value

- A notification can be dismissed from the system shade without removing its saved archive record.
- Search and filters make prior captured records easier to find.
- Bookmarks preserve selected records from normal retention cleanup.
- Local-first operation avoids sending notification content to a remote service.

## 6. MVP scope

### Required
1. Onboarding with plain-language explanation of notification access.
2. Link/button to Android notification access settings; verify state on return.
3. Capture supported notification posted/updated events while the listener is active.
4. Save app package, best-effort app label, title/text fields exposed by Android, event timestamps, and capture timestamp.
5. Inbox sorted newest-first.
6. Search across saved title/body/app label.
7. Filter by source app and date range.
8. Bookmark/unbookmark.
9. Open the source app when possible, without claiming a specific notification can always be reopened.
10. Delete individual records and clear archive.
11. Configurable retention (default 30 days; options 7/30/90 days and “keep until deleted”).
12. Exclude bookmarked records from automatic retention cleanup by default.
13. Per-app capture enable/disable.
14. Empty, loading, permission-disabled, and error states.
15. Local data export as an explicit user action only if included in the approved MVP release; export must be documented as sensitive. If export is not implemented, do not show a nonfunctional control.
16. Privacy notice and a way to clear stored data.

### Explicitly deferred
- Cloud backup/sync, multi-device sync, AI categorization/summaries, automatic reminders, widgets, browser dashboard, remote API, user accounts, paid plans.

## 7. Functional requirements

- FR-01: The app shall clearly state that notification access is optional and broad.
- FR-02: The app shall detect whether notification access is enabled and reflect it in UI.
- FR-03: When connected and authorized, the listener shall process supported posted/updated notification events.
- FR-04: The app shall store a local snapshot of allowed fields available at capture time.
- FR-05: Dismissal from the system shade shall not itself delete the archive record.
- FR-06: Listener removal events shall not automatically delete archived records.
- FR-07: Search shall return matching stored records without network access.
- FR-08: Users shall be able to filter by app and time range.
- FR-09: Users shall be able to bookmark and delete records.
- FR-10: Retention cleanup shall delete expired, non-bookmarked records according to settings.
- FR-11: Users shall be able to disable capture globally or per app.
- FR-12: If permission is revoked, the app shall stop claiming capture is active and show a recovery path.
- FR-13: The app shall not log payload text or transmit it off-device in MVP.
- FR-14: The app shall communicate that missing/redacted/unobserved content cannot be recovered.

## 8. Non-functional requirements

- NFR-01 Privacy: no notification payload sent off-device in MVP.
- NFR-02 Reliability: failures must be visible or safely handled; no silent false success.
- NFR-03 Responsiveness: local search target under 300 ms for 10,000 simple records on a mid-range test device; treat as a measured target, not a universal guarantee.
- NFR-04 Battery: no polling loop; event-driven listener and deferrable cleanup.
- NFR-05 Accessibility: meaningful labels, adequate contrast, scalable text, TalkBack support.
- NFR-06 Maintainability: MVVM, repository, DAO, documented migrations, tests.
- NFR-07 Compatibility: set min/target SDK based on the implementation date and current Play requirements; document the tested Android versions and devices.
- NFR-08 Storage: retention cleanup and user delete actions must be deterministic and tested.

## 9. Success metrics for closed beta

- At least 95% of scripted, eligible test events are archived when the listener is connected and the source exposes the tested fields. This is a test-harness target, not a promise for every device/app.
- All critical user journeys pass on emulator and at least one physical device.
- No notification payload appears in app logs during the test suite.
- Search returns correct results for defined test fixtures.
- No known critical/high security issue remains open.
- Users understand the notification-access disclosure in a short usability test.

## 10. Risks and assumptions

- Android and OEM behaviour differs.
- Source apps may redact or omit notification content.
- Grouped, ongoing, silent, updated, or custom notifications can behave differently.
- Notification access is highly sensitive and users may decline it.
- System may disconnect/rebind listener; no design can guarantee uninterrupted capture.
- Store policies and Android platform requirements can change; re-check before release.

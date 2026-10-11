# Database Schema and Data Dictionary

## 1. Storage baseline

Room/SQLite is the local system of record. No remote database in MVP. Persist only data needed for archive features. Store timestamps as UTC epoch milliseconds; format them in the device's local timezone for display.

## 2. `archived_notifications` table

Recommended baseline fields:

| Column | Type | Nullable | Notes |
|---|---|---:|---|
| `id` | TEXT (UUID) | No | App-owned primary key |
| `packageName` | TEXT | No | Source package identifier |
| `appLabel` | TEXT | Yes | Best-effort display label at capture time |
| `title` | TEXT | Yes | Normalized title if exposed |
| `body` | TEXT | Yes | Normalized body/text if exposed |
| `eventTimeMs` | INTEGER | No | Platform event time or documented fallback |
| `capturedAtMs` | INTEGER | No | Local capture time from injected clock |
| `isBookmarked` | INTEGER/Boolean | No | Default false |
| `isRead` | INTEGER/Boolean | No | Vault UI read state; distinct from source app state |
| `category` | TEXT | Yes | Optional deterministic category; null in initial MVP if unused |
| `expiresAtMs` | INTEGER | Yes | Optional computed expiry; alternatively calculate cutoff at cleanup |
| `dedupeFingerprint` | TEXT | Yes | Non-content or carefully scoped internal fingerprint; avoid storing raw notification keys as stable identity |
| `sourceNotificationKey` | TEXT | Yes | Optional ephemeral platform key; document lifecycle and avoid relying on permanence |

Use a stable UUID for every row. Do not make package + title + body a unique constraint; legitimate repeated alerts can share those values.

## 3. Settings

Use DataStore Preferences or a small Room settings table. Keep one source of truth. Suggested settings:
- `globalCaptureEnabled` (default true after explicit access setup; actual listener permission remains system-controlled)
- `retentionDays` (default 30; allowed values 7, 30, 90, or null for keep-until-deleted)
- per-package capture enablement
- `hideSensitivePreviews` (default true if implemented)
- optional user-selected app exclusions

Do not store the system permission state as authoritative persisted truth; re-check it from Android APIs.

## 4. Indexes and queries

Initial indexes:
- `(capturedAtMs DESC)` or equivalent query index for inbox order
- `packageName, capturedAtMs`
- `isBookmarked, capturedAtMs`
- optional `eventTimeMs` if used for date filtering

Search options:
- MVP: SQL `LIKE` across app label/title/body with parameterized queries and escaped wildcard handling.
- If performance measurements require it, consider SQLite FTS. FTS introduces synchronization/migration complexity and must be tested for deletion consistency.
- Do not build SQL strings from raw user input.

## 5. Deduplication and updates

The platform may post updates under the same key, but key behaviour is not a universal permanent identity. Design a best-effort policy:
- Within an active process/session, use source key when present to update a known record.
- Where key is missing, use a bounded time-window fingerprint with package and selected normalized fields.
- Never merge repeated identical alerts indefinitely.
- Test same-title repeated events, updates with changed body, group summaries, null fields and rapid bursts.
- Document known limitations in `docs/PRD.md`.

The exact algorithm must be selected and documented in code comments before implementation. If uncertain, prefer preserving separate records over accidentally overwriting distinct notifications.

## 6. Retention

- Default: 30 days.
- Choices: 7, 30, 90 days, keep-until-deleted.
- Cutoff uses `capturedAtMs` unless product review selects `eventTimeMs`.
- Automatic cleanup deletes expired non-bookmarked rows only.
- Explicit user delete and clear-all may delete bookmarked rows after confirmation.
- Cleanup is idempotent.
- Schedule with WorkManager as deferrable work; cleanup may run later than the exact cutoff.
- Keep-until-deleted disables automatic deletion but does not disable manual deletion.

## 7. Migration policy

- Increment Room schema version for schema changes.
- Add migration tests from every released schema to the new schema.
- Never use destructive migration in a production build containing user data unless there is an explicitly approved data-loss decision and user-facing warning.
- Export or backup, if later introduced, must have a versioned format and import validation.

## 8. Data deletion caveats

Deleting a row removes it from the app's active database. It does not guarantee forensic erasure from flash storage, OS backups, user-created exports, or device-level copies. If backup behaviour is configured, review it explicitly and communicate it accurately.

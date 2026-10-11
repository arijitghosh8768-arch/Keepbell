# User Stories and Use Cases

## Story format

Each story includes a priority and acceptance criteria. P0 is required for MVP; P1 is useful but can follow if time is constrained.

## US-01 — Understand access request (P0)

As a user, I want to understand what notification access means before enabling it.

Acceptance:
- Onboarding says the app can access notification content made available by Android.
- It says data is stored locally in MVP.
- It explains that capture is not retroactive and may be incomplete.
- The app does not pretend permission has been granted before system confirmation.

## US-02 — Enable notification capture (P0)

As a user, I want a direct route to Android's notification access settings.

Acceptance:
- A button opens the supported system settings screen.
- On returning, the app re-checks permission state.
- If still disabled, the app explains how to enable it without blocking access to unrelated screens.

## US-03 — View saved notifications (P0)

Acceptance:
- Records display newest-first.
- App label, title/body preview and capture/event time are shown when available.
- Missing title/body are handled gracefully.
- Empty archive has a useful explanation.

## US-04 — Search old notification (P0)

Acceptance:
- Search matches case-insensitive stored app label, title and body.
- Empty query returns the normal list.
- No-result state suggests clearing the query or filters.
- Search is local and does not send data to a server.

## US-05 — Filter by source app and date (P0)

Acceptance:
- App filter can be selected and cleared.
- Date boundaries are defined consistently using device-local display time and stored epoch timestamps.
- Combined search and filters intersect rather than override each other unexpectedly.

## US-06 — Bookmark important record (P0)

Acceptance:
- Bookmark state persists across process restart.
- Bookmarked records are excluded from automatic retention cleanup by default.
- User can still explicitly delete a bookmarked record.

## US-07 — Dismiss notification without losing archive (P0)

Acceptance:
- Dismissing the system notification does not delete the archive record.
- A listener removal callback only updates transient state if needed; it does not remove archived history.

## US-08 — Handle incomplete content (P0)

Acceptance:
- Missing/redacted fields are shown as unavailable rather than fabricated.
- The app does not claim it can retrieve a full source-app message.
- Tests cover null title and null body.

## US-09 — Manage retention (P0)

Acceptance:
- User can choose 7, 30, 90 days or keep-until-deleted.
- Cleanup deletes only records older than the cutoff and not bookmarked.
- The app clearly explains that retention is based on saved record timestamps and the selected policy.

## US-10 — Revoke access (P0)

Acceptance:
- On resume, access state is rechecked.
- UI indicates capture is disabled if access was revoked.
- Existing local records remain until user deletes them; revoking listener access does not imply archive deletion.
- Re-enabling access resumes future capture; missed interval is not backfilled.

## US-11 — Clear archive (P0)

Acceptance:
- Clear-all requires confirmation.
- Confirmation communicates that the action is irreversible within the app unless the user has separately exported data.
- The resulting archive is empty.

## US-12 — Open source app (P1)

Acceptance:
- A record action attempts to open the originating app only when a launch intent is available.
- Failure is handled without crashing.
- UI copy says “Open app,” not “Open exact message,” unless an app-specific deep link is explicitly implemented and tested.

## US-13 — Per-app exclusion (P0)

Acceptance:
- Disabling an app prevents future records from that package from being saved.
- It does not silently delete prior records.
- Settings persist after process restart.

## US-14 — Device restart / listener reconnection (P0)

Acceptance:
- After restart, the app reports actual permission/capture status.
- The app does not claim it captured notifications during an interval when disconnected.
- Listener reconnection is tested on target devices, while OEM differences are documented.

## US-15 — Export archive (P1; only if approved)

Acceptance:
- Export is a deliberate user action with a warning that the file may contain private content.
- Export destination uses Android's user-mediated document picker.
- No automatic upload occurs.
- Exported content format and included fields are documented.

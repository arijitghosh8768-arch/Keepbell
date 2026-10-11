# UX and Interaction Specification

## 1. Navigation

Bottom navigation:
- Inbox
- Bookmarks
- Settings

Search and app/date filters are available from Inbox. A record detail screen can be a route or bottom sheet, provided accessibility and navigation behaviour are consistent.

## 2. Screens

### Onboarding
- Product name and concise benefit.
- Explain notification access in plain language.
- State local-only MVP behaviour and capture limitations.
- Primary action: “Open notification access settings”.
- Secondary action: “Not now”.
- Do not repeatedly nag after refusal.

### Inbox
- Status banner: “Capture enabled” or “Notification access is off”.
- Search field.
- App/date filter controls.
- Notification cards: app label, title, body preview, timestamp, bookmark control.
- Sort newest-first.
- Pull-to-refresh is not required; records update reactively from database Flow.

### Bookmarks
- Only bookmarked records.
- Empty state explains how to bookmark.
- User can open or delete a record.

### Detail
- Full saved fields as captured, with unavailable values omitted or clearly marked.
- Source app and timestamps.
- Bookmark toggle.
- Open source app action where available.
- Delete action with confirmation if destructive.

### Settings
- Current notification access status and settings link.
- Global capture enabled/disabled.
- Per-app capture list based on observed sources or installed app metadata only as necessary.
- Retention: 7 / 30 / 90 days / keep until deleted.
- Hide sensitive previews toggle if implemented.
- Clear archive action.
- Privacy explanation and policy link.

## 3. UI states

Every screen that loads data must handle:
- Loading
- Content
- Empty
- Error
- Permission unavailable (where relevant)

No-result search state must distinguish “no matches” from “archive is empty”.

## 4. Copy rules

Use accurate phrases:
- “Saved notification”
- “Captured text”
- “Open app”
- “Notification access is off”
- “Some notifications may be missing or incomplete”

Avoid:
- “Recover every deleted message”
- “Restore any notification”
- “100% capture”
- “Open exact message” unless tested for a specific app and intent.

## 5. Accessibility

- Use semantic labels for icon-only actions.
- Support font scaling.
- Check contrast in light and dark themes.
- Minimum practical touch target around 48dp.
- Ensure TalkBack can distinguish bookmark/delete/open actions.
- Do not rely on colour alone to communicate capture state.

## 6. Destructive actions

- Delete one: confirm if product usability testing indicates accidental deletion risk; at minimum provide a clear action label.
- Clear all: confirmation required and must say archive records will be deleted.
- Clear-all should not be conflated with Android notification shade clear-all.
- Do not claim secure forensic erasure.

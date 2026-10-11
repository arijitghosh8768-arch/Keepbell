# Privacy, Platform Policy, and Compliance Mapping

**Purpose:** Engineering checklist, not legal advice. Requirements depend on distribution countries, target audience, app functionality, and current policy wording. Re-check official sources immediately before release.

## 1. Privacy principles

- Transparency: tell users what notification access allows.
- Purpose limitation: archive eligible notifications only for user retrieval.
- Data minimization: store only fields needed for features.
- Local-first: no remote transfer in MVP.
- User control: capture toggles, retention, delete, and permission revocation.
- Security: avoid payload logs and protect exports.
- Accuracy: do not claim recovery of content never exposed to the listener.

## 2. Data inventory

| Data | Source | Purpose | MVP location | Network transfer |
|---|---|---|---|---|
| Package name/app label | Android notification/app metadata | Identify source app | Local Room | None |
| Title/body text | Fields exposed by notification | Search and archive preview | Local Room | None |
| Event/capture timestamps | Android event + app clock | Sort/filter/retention | Local Room | None |
| Bookmark/read state | User interaction | Organize archive | Local Room | None |
| Capture/retention preferences | User settings | App behaviour | Local settings | None |
| Export file, if feature enabled | User-initiated export | User portability | User-selected destination | None by app; destination is user-controlled |

## 3. Compliance and policy review matrix

| Area | What to do | Evidence to retain |
|---|---|---|
| Android notification access | Use supported `NotificationListenerService` mechanism and system settings flow | Manifest review, permission-flow test |
| Google Play user data / sensitive permissions | Review current User Data policy and permission guidance; ensure prominent disclosure and consent where required | Policy review date, disclosure screenshots |
| Google Play Data safety | Complete declarations based on actual app and SDK behaviour; do not assume local-only automatically means no form is required | Submitted form copy and audit |
| Privacy policy | Publish an accurate policy describing notification access, local storage, retention, deletion, export and contact channel | Versioned policy and URL |
| Data deletion | Provide clear in-app deletion; if future account/cloud data exists, design applicable deletion flow | Test evidence |
| Children/target audience | Declare target audience accurately and review applicable family/child policies | Release checklist |
| Intellectual property | Use properly licensed icons, fonts, screenshots and dependencies | Licence inventory |
| Accessibility | Test text scaling, contrast, TalkBack, touch targets and semantics | Accessibility test report |
| Security | Maintain dependency hygiene, no secrets, payload-free logs | CI reports and review notes |
| Jurisdictional privacy laws | Determine applicability of Indian DPDP Act/rules and any other laws based on launch scope; obtain qualified legal advice if needed | Legal review where appropriate |

## 4. Required user-facing disclosure content

Before sending the user to system settings, explain in plain language:
- Android notification access may let this app read notification content and metadata exposed by other apps.
- The MVP stores eligible captured fields on this device for history and search.
- Content may be missing, redacted, or unavailable.
- Capture begins only after access is enabled and while the listener receives events.
- Revoking access stops future capture but does not automatically delete the existing archive.
- The user can clear stored records in the app.
- No notification content is sent to a remote server by the MVP.

Do not present a custom “consent” button as if it replaces the system-level settings permission. The app should offer a clear choice and respect refusal.

## 5. Official sources to verify before release

Use the current versions of:
- Android `NotificationListenerService` API reference: https://developer.android.com/reference/android/service/notification/NotificationListenerService
- Android notification listener guide: https://developer.android.com/develop/ui/views/notifications/notification-listener
- Google Play User Data policy: https://support.google.com/googleplay/android-developer/answer/10144311
- Google Play Data safety section guidance: https://support.google.com/googleplay/android-developer/answer/10787469
- Google Play Developer Program Policies: https://play.google.com/about/developer-content-policy/
- Android app signing guidance: https://developer.android.com/studio/publish/app-signing
- India's Digital Personal Data Protection Act, 2023 official text and current rules/notifications, if India is a target market: verify on official Government of India sources.

URLs and policy details can change. Confirm that each source is current on the actual release date.

## 6. Release compliance gate

Do not publish until:
- Actual permission and data flows have been audited.
- Privacy policy matches the final build and included SDKs.
- Play Console declarations match actual behaviour.
- User disclosure is visible before notification access is requested.
- No unsupported guarantee about capturing every notification is made.
- Target audience and countries are decided.

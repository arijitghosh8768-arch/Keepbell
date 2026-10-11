# Notification Vault — Project Documentation Pack

**Status:** Implementation baseline v1.0  
**Prepared:** 2026-10-11  
**Platform:** Android  
**Primary stack:** Kotlin, Jetpack Compose, Room/SQLite, MVVM, Repository pattern, `NotificationListenerService`  
**AI coding environment:** Google Antigravity  
**Product posture:** Local-first, privacy-first, no account and no backend in MVP

## What this project is

Notification Vault is an Android application that lets a user archive eligible notifications received after notification access has been enabled, then search, filter, bookmark, and delete archived records. It does not recover notifications that were never observed, content that an originating app did not expose, or messages removed before capture.

## Start here

Read the documents in this order:

1. `docs/PRD.md` — product scope and acceptance criteria
2. `docs/USER_STORIES.md` — user workflows and edge cases
3. `docs/ARCHITECTURE.md` — components and data flow
4. `docs/DATABASE.md` — Room schema and migration rules
5. `docs/SECURITY_THREAT_MODEL.md` — threats, mitigations, residual risks
6. `docs/PRIVACY_AND_COMPLIANCE.md` — privacy and store-policy review checklist
7. `docs/UX_SPEC.md` — screens, states, navigation
8. `docs/TEST_PLAN.md` — verifiable quality gates
9. `docs/IMPLEMENTATION_PLAN.md` — task-by-task build sequence
10. `docs/AI_WORKFLOW_ANTIGRAVITY.md` — instructions for the AI coding agent
11. `docs/CI_CD_RELEASE.md` — CI, signing, release and rollback
12. `docs/OPERATIONS_AND_SUPPORT.md` — support, incidents and maintenance
13. `docs/ACCEPTANCE_MATRIX.md` — requirement-to-test traceability
14. `docs/decisions/ADR-001-local-first.md` — architectural decision record

## Non-negotiable product constraints

- Android-only MVP; do not promise equivalent iOS behaviour.
- The user must explicitly enable Android notification access in system settings.
- Capture is best-effort and begins only while the listener is connected and permitted.
- Archived text is a snapshot of what Android made available at capture time, not a guaranteed copy of an entire message.
- Do not bypass app privacy settings, notification redaction, lock-screen privacy, or Android permission controls.
- Local storage only for MVP. No analytics SDK, ad SDK, cloud backup, account, or network permission unless separately approved through an ADR and privacy review.
- Never log notification title/body, OTPs, private message text, or other notification payloads.
- Do not silently send archived content to an AI service. Any future AI/cloud feature requires explicit opt-in and a separate threat/privacy review.
- A feature is not complete until code, tests, documentation, and acceptance criteria agree.

## Project structure

```text
NotificationVault/
├── README.md
├── AI_WORKFLOW.md
├── CONTRIBUTING.md
├── CHANGELOG.md
├── .gitignore
├── .editorconfig
├── docs/
│   ├── PRD.md
│   ├── USER_STORIES.md
│   ├── ARCHITECTURE.md
│   ├── DATABASE.md
│   ├── SECURITY_THREAT_MODEL.md
│   ├── PRIVACY_AND_COMPLIANCE.md
│   ├── UX_SPEC.md
│   ├── TEST_PLAN.md
│   ├── IMPLEMENTATION_PLAN.md
│   ├── CI_CD_RELEASE.md
│   ├── OPERATIONS_AND_SUPPORT.md
│   ├── ACCEPTANCE_MATRIX.md
│   ├── decisions/ADR-001-local-first.md
│   └── diagrams/system-context.mmd
└── .github/workflows/android-ci.yml
```

## Definition of MVP done

The MVP is ready for a closed beta only when:
1. The app builds from a clean checkout using the documented toolchain.
2. The user can understand and grant/revoke notification access.
3. Eligible notifications are persisted locally while capture is active.
4. Inbox, search, filters, bookmarks, deletion, and retention work.
5. Tests pass on emulator and at least one physical device.
6. Permission loss, restart, database errors, redacted content, duplicates, and notification updates are tested.
7. No sensitive notification content appears in logs or crash reports.
8. Privacy policy and applicable distribution disclosures match actual behaviour.
9. A signed release artifact is produced without committing secrets.
10. Known limitations are visible in the app and release notes.

This pack is an engineering baseline, not legal advice or a guarantee of Google Play approval. Verify current Android SDK and Play requirements before release.

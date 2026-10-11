# ADR-001: Local-First MVP

- **Status:** Accepted
- **Date:** 2026-10-11
- **Decision owners:** Project owner and implementation reviewer

## Context

Notification content can be highly sensitive. The initial product value is local search and retention, which does not require a backend. A remote service would add authentication, network security, consent, deletion, compliance, operational cost and breach exposure.

## Decision

Use Room/SQLite on the Android device as the only source of truth in MVP. Do not add accounts, cloud sync, analytics, ad SDKs, AI APIs, or network permission unless a later reviewed ADR changes the decision.

## Consequences

Positive:
- No server operating cost.
- No remote notification-content transfer in MVP.
- Simpler architecture and privacy explanation.
- Offline search works.

Negative:
- No cross-device sync.
- Local archive can be lost if app data/device is removed.
- Local-only does not guarantee protection against compromised/unlocked devices.
- Backup/export needs a separate user-controlled design.

## Revisit triggers

Revisit only if validated users require sync/backup or a product requirement cannot be met locally. Any change requires updated threat model, data inventory, privacy disclosures, tests, and consent UX.

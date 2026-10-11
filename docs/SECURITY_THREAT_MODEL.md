# Security Threat Model

**Classification:** Sensitive personal data application  
**Method:** Lightweight STRIDE-inspired review  
**Scope:** MVP local Android app, notification listener, Room database, UI, export only if implemented

## 1. Assets

- Notification titles and bodies
- Sender/app identifiers
- Timestamps and behavioural patterns
- Bookmarks and user settings
- Exported archive, if supported
- Build signing key and release credentials
- Source code and dependency supply chain

## 2. Trust boundaries

1. Source app → Android notification manager
2. Android notification manager → user-authorized listener
3. Listener → app process and local database
4. Database → UI / search
5. App → user-selected export destination, if implemented
6. Developer environment / CI → signed release artifact

MVP has no app server boundary because no remote service is permitted.

## 3. Threats and controls

| ID | Threat | Impact | Required mitigation | Verification |
|---|---|---|---|---|
| T-01 | User does not understand broad notification access | Unexpected exposure / loss of trust | Plain-language disclosure before settings link; no dark patterns | Usability review |
| T-02 | Other person uses unlocked phone to read archive | Privacy breach | Optional app lock, hide previews, clear archive control; assess app-lock limits | Manual device test |
| T-03 | Payload appears in logs/crash reports | Sensitive text disclosure | Never log payload; sanitize exceptions; no content analytics | Logcat review using sentinel string |
| T-04 | Notification content sent to network | High privacy impact | No `INTERNET` permission or network SDK in MVP; review dependency/manifest | Manifest/dependency audit |
| T-05 | Malicious or malformed notification content | Crash, UI injection, resource exhaustion | Treat all fields as untrusted text; no HTML execution; cap stored lengths; catch malformed extras safely | Fuzz/unit tests |
| T-06 | Database corruption or migration failure | Data loss | Migration tests, backups only if explicitly designed, no destructive migration | Migration test suite |
| T-07 | Exported file exposed | Private archive leak | Explicit user action, warning, system document picker, no auto-upload | Export review/test |
| T-08 | Dependency compromise | Supply-chain compromise | Pin/verify dependencies, review updates, CI checks, no arbitrary binaries | Dependency review |
| T-09 | Signing key leaked | Malicious release / update compromise | Keep keys outside repo; use CI secrets or secure local storage | Secret scan and release checklist |
| T-10 | False claim of complete capture | User relies on missing data | Clear limitation copy; status state; no “all notifications guaranteed” claims | Copy review and edge tests |
| T-11 | App captures its own future notifications | Infinite/irrelevant archive noise | Exclude own package or allow explicit control | Integration test |
| T-12 | Retention cleanup deletes important record | Data loss | Bookmark protection; preview cutoff; tests for boundaries | DAO/worker tests |
| T-13 | Rooted/compromised device reads local data | Confidentiality loss | Minimize storage, optional app lock, avoid overstating protection | Threat disclosure |
| T-14 | Clipboard or screenshots expose content | Secondary disclosure | Avoid automatic clipboard use; consider screenshot protection only where justified and explain trade-offs | Manual review |
| T-15 | Listener disconnected by OS/OEM | Missing archive records | Detect permission state on resume, show status, document no backfill | Device test |

## 4. Data minimization rules

- Do not store icon bitmaps, large attachments, raw framework objects, or notification action RemoteViews unless a documented requirement is approved.
- Do not store sender/phone/email separately unless exposed and needed; default to title/body fields as provided.
- Do not infer OTPs or credentials into separate searchable metadata.
- Bound maximum field lengths to protect against oversized payloads; define constants and test truncation.
- Never upload data in MVP.
- Do not add analytics/ad/crash SDKs that could receive payloads. If crash reporting is introduced later, verify data scrubbing and update disclosures.

## 5. Encryption decision

MVP should minimize data and use Android app-private storage. App-private storage is not equivalent to database-level encryption against a compromised/rooted device or all unlocked-device threats. Do not claim the database is encrypted unless an actual, maintained encryption design is implemented and verified.

If encryption is added:
- Select a maintained implementation after compatibility review.
- Define key creation, Android Keystore use, backup/restore behaviour, lock-screen changes, key invalidation, migration, and recovery.
- Test key loss and upgrade paths.
- Never invent a custom cryptographic scheme.
- Update ADR and privacy documentation.

## 6. Residual risks

- The OS may expose incomplete/redacted content.
- A malicious/compromised device can undermine local protections.
- Notification access itself is a high-trust permission.
- Local archive may expose patterns about the user's life.
- User-created exports can escape app control.
- OEM background management can cause capture gaps.

## 7. Security acceptance gate

Before beta:
- No known critical/high issue open.
- No payload text in Logcat during sentinel tests.
- No unapproved network permission/SDK.
- Delete and retention tests pass.
- Listener permission explanation reviewed.
- Threat model updated for every new data flow.

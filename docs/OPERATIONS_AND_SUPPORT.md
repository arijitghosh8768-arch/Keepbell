# Operations, Support, and Incident Response

## 1. MVP operating model

No server is operated in MVP. Operational responsibilities still include release monitoring, bug triage, dependency updates, user support, and privacy incident response.

## 2. Support intake

Ask users for:
- App version
- Android version and device manufacturer/model
- Whether notification access is currently enabled
- Reproducible steps and approximate time
- Sanitized logs only, after reviewing them for private data

Never ask a user to send their notification archive, OTPs, private message screenshots, or unredacted database unless there is a separately reviewed, strictly necessary support process. Default answer: do not collect notification content.

## 3. Severity guide

- S0 Critical: confirmed broad sensitive-data exposure, malicious release, or severe data corruption affecting many users.
- S1 High: capture/privacy behaviour materially violates documented contract; significant data loss or crash loop.
- S2 Medium: core feature broken for some devices; workaround exists.
- S3 Low: cosmetic issue or minor usability problem.

## 4. Incident response checklist

1. Stop distribution or pause rollout if feasible.
2. Record incident time, affected versions and known scope without copying notification payloads.
3. Preserve relevant sanitized build/CI evidence.
4. Determine whether data left the device; do not assume it did not—verify code and dependencies.
5. Fix, test, and obtain review.
6. Determine notification obligations under applicable law/platform policy with qualified advice.
7. Publish an accurate user-facing notice if needed.
8. Add regression tests and update threat model.
9. Conduct a short post-incident review.

## 5. Maintenance cadence

- Each release: build/test/security checklist.
- Monthly or before each release: review critical dependencies and Android compatibility.
- On platform/policy changes: update compliance mapping.
- Every material feature: update PRD, architecture, threat model, test plan and changelog.

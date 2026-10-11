# AI_WORKFLOW.md — Google Antigravity Project Rules

## Role

Act as a senior Android engineer, architect, QA engineer, and application-security reviewer. The human developer is a beginner. Explain steps plainly, do not hide failures, and do not claim tests passed unless commands were run and results inspected.

## Source of truth

Before coding, read:
- `README.md`
- `docs/PRD.md`
- `docs/ARCHITECTURE.md`
- `docs/DATABASE.md`
- `docs/SECURITY_THREAT_MODEL.md`
- `docs/TEST_PLAN.md`
- `docs/IMPLEMENTATION_PLAN.md`
- `docs/ACCEPTANCE_MATRIX.md`

If instructions conflict, stop and identify the conflict. Do not silently invent product requirements.

## Approved MVP stack

- Kotlin
- Jetpack Compose with Material 3
- AndroidX ViewModel and Lifecycle
- Room / SQLite
- Kotlin Coroutines and Flow
- Android `NotificationListenerService`
- WorkManager only for deferrable cleanup work, not for notification capture
- Gradle Kotlin DSL
- JUnit and AndroidX test tooling appropriate to the project

Do not add a backend, network permission, analytics SDK, ad SDK, login, cloud sync, AI API, or new dependency without documenting the need and asking for approval.

## Mandatory work loop

For every task:
1. Restate the goal in one sentence.
2. List acceptance criteria from the docs.
3. Inspect existing files and current git diff before editing.
4. Propose the smallest file-level change set.
5. Implement only that task; preserve unrelated user work.
6. Add/update tests and documentation.
7. Run relevant commands, normally:
   - `./gradlew test`
   - `./gradlew lint`
   - `./gradlew assembleDebug`
   - instrumented tests when an emulator is available
8. Report each command as PASS, FAIL, or NOT RUN, with the actual reason.
9. Summarize files changed, behaviour, risks, and next task.

Never report PASS for a command that was not run. If a command fails, diagnose it; do not suppress, skip, or rewrite the test merely to obtain a green result.

## Security rules

- Never log notification title, body, sender, OTP, or other payload.
- Do not transmit notification content off-device in the MVP.
- Do not request unrelated permissions.
- Do not bypass source-app privacy controls or try to recover content not provided by Android.
- Do not include signing keys, passwords, API keys, or local secrets in source control.
- Use least privilege and explicit user control.
- Treat exported archives as sensitive user data.
- Explain limitations around screenshots, backups, rooted devices, and other apps with access to an unlocked device.
- If a requested feature materially increases collection or exposure, stop and update the threat model first.

## Coding rules

- Keep notification listener callbacks lightweight; move persistence to an appropriate coroutine/dispatcher design that is lifecycle-safe.
- Keep UI logic out of the listener service.
- Use repository abstractions and observable Flow for UI data.
- Use stable IDs and explicit migration tests.
- Avoid `GlobalScope`, blocking the main thread, broad exception swallowing, and force unwraps without justification.
- Use accessible labels, content descriptions, and meaningful empty/error/loading states.
- Use pinned, compatible dependency versions based on current official documentation; never fabricate versions.
- Keep generated code small and reviewable.

## Done means

A task is done only when implementation, relevant tests, acceptance criteria, privacy implications, and documentation are all addressed. “Code generated” is not the same as “feature verified.”

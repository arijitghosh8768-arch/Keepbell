# Implementation Plan — Beginner-Friendly, AI-Assisted

## Rules

- Work in small tasks. Do not ask AI to build the entire app in one pass.
- After each task, build and test before moving on.
- Commit only after reviewing the diff.
- Use a real Android device for notification-listener verification; emulator-only success is insufficient.

## Phase 0 — Environment and repository

### Task 0.1: Inspect tools
- Install a stable Android Studio version and its bundled compatible JDK.
- Install Android SDK platform/build tools required by the selected project.
- Create a Git repository.
- Record OS, Android Studio version, JDK, compile SDK, target SDK, and tested device versions in `docs/BUILD_ENVIRONMENT.md`.

Acceptance: a clean empty Android project opens and builds.

### Task 0.2: Create app skeleton
- Kotlin, Compose, Material 3.
- Add package structure from `ARCHITECTURE.md`.
- Create a basic Home screen.
Acceptance: `assembleDebug` passes and app launches.

## Phase 1 — Persistence

### Task 1.1: Define entity and DAO
- Implement notification entity.
- Add insert/update/delete/list queries.
- Add search and app/date filter queries.
Acceptance: DAO tests cover empty state, insert, update, filter, delete.

### Task 1.2: Database and repository
- Add Room database and repository interface/implementation.
- Expose observable Flow.
Acceptance: data persists across process restart and repository tests pass.

## Phase 2 — Permission and capture

### Task 2.1: Permission explainer
- Build onboarding and access status.
- Link to Android notification access settings.
- Recheck state when app resumes.
Acceptance: enabled/disabled states are accurate.

### Task 2.2: Listener adapter
- Declare service in manifest according to official API.
- Extract only approved fields.
- No payload logs.
Acceptance: synthetic test notification is saved on a physical device.

### Task 2.3: Capture processor
- Apply global/per-app settings.
- Normalize null fields and cap sizes.
- Implement documented dedupe/update policy.
Acceptance: TC-002 through TC-012 pass where automatable; remaining device tests recorded.

## Phase 3 — User features

### Task 3.1: Inbox and detail
Acceptance: chronological list and safe detail view.

### Task 3.2: Search and filters
Acceptance: combined filters and edge-case search tests pass.

### Task 3.3: Bookmarks and deletion
Acceptance: bookmark persists; explicit deletion works.

### Task 3.4: Retention
Acceptance: cleanup policy tests pass; worker is idempotent.

### Task 3.5: Settings and privacy states
Acceptance: access, per-app capture, retention, clear-all are functional.

## Phase 4 — Reliability and hardening

- Add migration tests.
- Add malformed/oversized notification tests.
- Verify no payload in logs.
- Audit manifest and dependencies.
- Test process death, reboot, permission revoke/re-enable.
- Measure search latency on a documented dataset.
Acceptance: all P0 test cases pass or are tracked with approved exception.

## Phase 5 — Beta

- Prepare privacy policy and applicable store declarations.
- Build signed artifact securely.
- Test clean install and upgrade.
- Run a small closed beta and gather non-sensitive feedback.
- Triage bugs; update changelog and release notes.
Acceptance: release gate in `CI_CD_RELEASE.md` passes.

## Daily AI task template

“Read `AI_WORKFLOW.md` and the relevant requirements. Implement only [TASK ID]. First list acceptance criteria and files to change. Inspect the current project and diff. Implement the smallest coherent change, add tests, run the relevant Gradle commands, and report actual PASS/FAIL/NOT RUN results. Do not claim success for unrun tests. Update documentation if behaviour changes.”

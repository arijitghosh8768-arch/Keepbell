# Contributing

## Before making changes
- Read `AI_WORKFLOW.md` and relevant documents under `docs/`.
- Create a branch for each feature or bug fix.
- Keep the change focused.

## Code expectations
- Kotlin style consistent with the existing project.
- No payload text in logs or tests using real personal data.
- Update tests and docs with behavioural changes.
- Do not add permissions or dependencies without review.
- Avoid destructive database migrations.

## Pull request checklist
- [ ] Requirements and acceptance criteria identified
- [ ] Tests added/updated
- [ ] `./gradlew test` run
- [ ] `./gradlew lint` run
- [ ] `./gradlew assembleDebug` run
- [ ] No private data or secrets in diff/logs
- [ ] Threat model/privacy docs updated if data flow changed
- [ ] Changelog updated when user-visible behaviour changes

# Requirements-to-Test Acceptance Matrix

| Requirement | Primary document | Verification | Priority |
|---|---|---|---|
| Explain notification access | PRD FR-01; UX Spec | UX review; TC-001 | P0 |
| Correct access status | PRD FR-02/FR-12 | TC-001, TC-011, TC-012 | P0 |
| Capture eligible event | PRD FR-03/FR-04 | TC-002, physical device | P0 |
| Archive survives shade dismissal | PRD FR-05/FR-06 | TC-003 | P0 |
| Search locally | PRD FR-07 | TC-016, TC-017 | P0 |
| App/date filtering | PRD FR-08 | TC-018 | P0 |
| Bookmark/delete | PRD FR-09 | TC-013, TC-019, TC-020 | P0 |
| Retention policy | PRD FR-10 | TC-014, TC-015, TC-031 | P0 |
| Global/per-app capture control | PRD FR-11 | TC-009, TC-010 | P0 |
| No payload logging | PRD FR-13; Threat T-03 | TC-025 | P0 |
| Honest limitation messaging | PRD FR-14; UX copy rules | Copy review; TC-004/005 | P0 |
| Migration preserves data | Database migration policy | TC-023 | P0 |
| Build/lint/unit tests pass | CI/CD | CI logs | P0 |
| No unapproved permissions | Architecture; Threat T-04 | TC-026; manifest review | P0 |
| Export safely (if included) | PRD; Privacy; Threat T-07 | TC-032 | P1 |
| Open source app gracefully | User story US-12 | TC-030 | P1 |
| Performance target | PRD NFR-03 | Benchmarked dataset report | P1 |

## Release decision record

Before beta, fill:
- Build commit:
- App version/versionCode:
- Tested Android versions:
- Physical devices:
- Unit test result:
- Lint result:
- Debug/release build result:
- P0 exceptions (owner, reason, due date):
- Open security findings:
- Privacy policy reviewed on:
- Play policy reviewed on:
- Decision: GO / NO-GO
- Reviewer:
- Date:

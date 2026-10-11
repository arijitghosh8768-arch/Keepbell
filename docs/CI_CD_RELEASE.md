# CI/CD, Signing, and Release Plan

## 1. Goals

- Reproducible build from a clean checkout.
- Automated unit tests, lint and debug build on pull requests.
- No secrets in repository or build logs.
- Signed release artifacts created only from a controlled release process.
- Clear beta/production gates and rollback strategy.

## 2. Suggested CI stages

Pull request:
1. Checkout source.
2. Set up a JDK version compatible with the selected Android Gradle Plugin.
3. Configure Android SDK.
4. Run unit tests.
5. Run Android lint.
6. Assemble debug APK.
7. Optional dependency/secret scanning.
8. Upload sanitized test/build reports only.

Release:
1. Require reviewed/approved tag or release branch.
2. Repeat tests and lint.
3. Inject signing material through secure CI secrets only if using CI signing.
4. Build signed release artifact.
5. Verify artifact and version metadata.
6. Publish to internal/closed testing only after human approval.
7. Preserve checksums and release notes; do not publish test databases or private logs.

## 3. Example GitHub Actions workflow

The accompanying `.github/workflows/android-ci.yml` is a starter template. Pin action versions to reviewed immutable commit SHAs before using it for production. Verify Java and SDK versions against the actual Gradle configuration. CI configuration may need changes as Android Gradle Plugin versions evolve.

## 4. Signing security

- Never commit `.jks`, `.keystore`, signing passwords, service-account JSON, or API tokens.
- Add local signing files to `.gitignore`.
- Prefer Play App Signing for Play distribution, after reviewing current official guidance.
- Store upload key and passwords in a managed secret store.
- Restrict release workflow permissions and use environment approval where available.
- Rotate exposed credentials and follow incident procedure if a secret leaks.

## 5. Versioning

Use semantic-style versions for human communication:
- `versionName`: e.g. `0.1.0-beta01`
- `versionCode`: monotonically increasing integer required by Android distribution

Keep `CHANGELOG.md` updated. Do not reuse a version code for a new uploaded build.

## 6. Release gates

### Internal build
- Compiles and launches.
- No secrets committed.
- Core unit tests pass.

### Closed beta
- All P0 acceptance criteria pass.
- Real-device listener tests pass.
- Privacy disclosure and policy review complete.
- No known critical/high security issue.
- Signed artifact installed and smoke-tested.

### Production
- Target SDK and Play policy requirements rechecked.
- Data safety and privacy policy match final binary and included SDKs.
- Crash-free stability and battery/storage behaviour reviewed.
- Support contact and incident process ready.
- Rollback/release pause process documented.

## 7. Rollback

For a severe regression:
- Pause staged rollout or halt further distribution where platform controls allow.
- Identify affected versions and impact.
- Prepare a fixed version with a higher version code.
- Communicate clearly to testers/users.
- Do not remotely delete local archives or collect notification content to investigate.

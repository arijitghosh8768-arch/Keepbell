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

## 3. GitHub Actions workflows

The project maintains two distinct GitHub Actions workflows with strict permission separation:

1. **Pull Request & Branch CI** (`.github/workflows/android-ci.yml`):
   - Automatically triggered on pull requests and pushes to `main`.
   - Runs unit tests, lint, and builds `assembleDebug`.
   - Never has access to release signing secrets.
2. **Release Build & Verification** (`.github/workflows/android-release.yml`):
   - Triggered manually via `workflow_dispatch` or on release tag pushes (`v*`).
   - Injects release credentials through encrypted repository secrets into runner temp storage.
   - Enforces strict signing (`REQUIRE_RELEASE_SIGNING=true`).
   - Runs tests, lint, signed assembly, and verifies the resulting APK with `apksigner`.
   - Computes SHA-256 checksums and uploads artifacts with a 14-day retention.
   - **Does not publish public releases automatically**: Distribution requires manual human approval and physical-device verification.

## 4. Signing security and CI configuration

### GitHub repository secrets configuration
To enable the release workflow in GitHub, configure the following encrypted repository secrets under **Settings > Secrets and variables > Actions**:

| Secret Name | Description | Encoding / Source |
| :--- | :--- | :--- |
| `RELEASE_KEYSTORE_BASE64` | Base64-encoded release `.keystore` / `.jks` file | `base64 -w 0 release.keystore` (Linux/macOS)<br>`[Convert]::ToBase64String([IO.File]::ReadAllBytes('release.keystore'))` (PowerShell) |
| `RELEASE_KEYSTORE_PASSWORD` | Password for the release keystore | Plain text secret string |
| `RELEASE_KEY_ALIAS` | Alias name of the signing key entry | Plain text secret string |
| `RELEASE_KEY_PASSWORD` | Password for the private signing key | Plain text secret string |

*Never commit real passwords, base64 keystore strings, or keystore files to the repository.*

### Credential sources and precedence
The Gradle build resolves credentials in the following order:
1. **Environment Variables**:
   - `KEYSTORE_PATH`: Path to the `.jks` or `.keystore` file (resolved relative to repository root or absolute).
   - `KEYSTORE_PASSWORD`: Keystore password.
   - `KEY_ALIAS`: Key alias.
   - `KEY_PASSWORD`: Key password.
2. **Local Untracked Configuration** (`keystore.properties` in repository root):
   - `storeFile`: Path to keystore file.
   - `storePassword`: Keystore password.
   - `keyAlias`: Key alias.
   - `keyPassword`: Key password.
   *(See `keystore.properties.example` for the template).*
3. **Gradle Project Properties** (e.g., `-PstoreFile=...`).

### Build behavior and enforcement
- **Default Local Pre-Flight**: When no signing credentials are provided and strict mode is off, `assembleRelease` produces an explicitly unsigned APK (`app-release-unsigned.apk`). Debug signing is never used as a fallback for release builds.
- **Strict Enforcement**: When `REQUIRE_RELEASE_SIGNING=true` (or `-PrequireReleaseSigning=true`) is set, Gradle immediately fails the build if credentials are missing or incomplete.
- **Partial Credential Safeguard**: If any credential is supplied but not all four, or if the specified keystore file does not exist, the build fails fast with a safe error message that never prints secret values.

### How to trigger a release build
- **GitHub Web UI**: Go to **Actions** > **Android Release Build** > click **Run workflow** > optionally enter the version tag (e.g., `v0.1.0-beta01`) > click **Run workflow**.
- **GitHub CLI**: `gh workflow run android-release.yml -f release_tag=v0.1.0-beta01`
- **Git Tag Push**: `git tag v0.1.0-beta01 && git push origin v0.1.0-beta01`

### Downloading and independently verifying the artifact
1. Download the artifact bundle `keepbell-release-apk` from the completed workflow run.
2. Unzip the bundle to obtain `app-release.apk` and `app-release.apk.sha256`.
3. Verify the checksum:
   ```powershell
   # PowerShell
   (Get-FileHash -Path app-release.apk -Algorithm SHA256).Hash
   Get-Content app-release.apk.sha256
   ```
   ```bash
   # Linux / macOS
   sha256sum -c app-release.apk.sha256
   ```
4. Verify signature validity and signing certificate:
   ```bash
   apksigner verify --verbose --print-certs app-release.apk
   ```
   Confirm that:
   - `Verifies: true`
   - Required signing schemes (v1, v2, v3) are enabled.
   - Signer certificate matches the intended organization and is not the default Android debug certificate.

### Why automated publishing is disabled
Keepbell explicitly avoids auto-publishing APKs to GitHub Releases or app stores on build completion. This design enforces an essential human quality and safety gate:
- Notification listener permissions, service bindings, reboot restoration, and battery-optimization behavior cannot be validated inside a headless CI runner.
- Every release candidate must undergo physical-device smoke testing and privacy policy review before distribution.

### Keystore backup and preservation
- The signing key defines the cryptographic identity of the application.
- Android strictly prohibits updating an installed app with an APK signed by a different key without a full uninstall (which permanently wipes the user's local notification database).
- Store backup copies of the release keystore and its passwords in secure, redundant, off-repository secret storage (such as an encrypted password manager or hardware security module).

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

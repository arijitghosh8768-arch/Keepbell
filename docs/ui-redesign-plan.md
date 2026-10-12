# Notification Vault - UI Redesign Plan

## 1. Objectives & Problems to Address
1. **Top Bar**: Currently tall and overly tinted, wasting vertical space. Replace with sleek, compact Material 3 TopAppBar with neutral styling.
2. **Capture Status Banner**: Currently heavy like a large card. Redesign to be slim, subtle, and clearly reflect real permission status with distinct active vs. inactive states and "Open settings" action.
3. **Card Differentiation (Read vs Unread)**: Currently identical. Unread cards will feature a bolder title, subtle primary accent marker, and prominent surface. Read cards will use calmer tonal surfaces.
4. **App Label Resolution & Caching**: Off-thread resolution via an async cached resolver helper (`AppMetadataResolver`), falling back to package name safely without blocking recomposition or the main thread.
5. **App Icon & Visual Identity**: Load source app icons asynchronously with smooth caching, displaying a clean monogram/letter-avatar fallback if uninstalled or unavailable.
6. **Timestamps**: Change long dates to compact formats ("10:45 AM" for today, "Yesterday", or "Oct 12") on cards while keeping full timestamps on the detail screen.
7. **Graceful Empty/Missing Text**: Compact, elegant layout for notifications without titles or bodies without awkward gaps.
8. **Bookmark Action**: Upgrade bookmark star to an explicit 48dp touch target with smooth filled/outlined icons (`Icons.Filled.Star` vs `Icons.Outlined.Star`).
9. **Launcher Icon**: Design a clean adaptive launcher icon vector (background + foreground + monochrome) representing a secure notification vault.
10. **State Views & Transitions**: Elegant empty, loading, error, and permission states across Inbox, Details, and Onboarding.

## 2. Components & Files to Create / Modify

### Theme & Design Tokens
- `app/src/main/java/com/example/notificationvault/ui/theme/Color.kt`: Refined Material 3 palette (sophisticated neutral tones, deep slate dark mode, restrained indigo/teal accent).
- `app/src/main/java/com/example/notificationvault/ui/theme/Type.kt`: Full Material 3 typography scale with crisp hierarchy.
- `app/src/main/java/com/example/notificationvault/ui/theme/Shape.kt`: Material 3 shape tokens.
- `app/src/main/java/com/example/notificationvault/ui/theme/Theme.kt`: Complete light/dark theme wiring with dynamic color support and contrast compliance.

### Reusable UI Components & Helpers
- `app/src/main/java/com/example/notificationvault/ui/common/AppMetadataResolver.kt`: Asynchronous coroutine-cached resolver for app labels and icons off the main thread.
- `app/src/main/java/com/example/notificationvault/ui/common/AppIcon.kt`: Async source app icon component with smooth monogram fallback.
- `app/src/main/java/com/example/notificationvault/ui/common/CaptureStatusBanner.kt`: Redesigned slim and subtle capture status indicator.
- `app/src/main/java/com/example/notificationvault/ui/common/StateViews.kt`: Reusable empty states, loading indicators, and error views.
- `app/src/main/java/com/example/notificationvault/util/DateTimeFormatter.kt`: Update with compact relative/time formatting for cards while keeping full format for details.

### Screen Redesigns
- `app/src/main/java/com/example/notificationvault/ui/inbox/InboxScreen.kt`:
  - Upgraded `NotificationCard` with app icon, app label + compact time, unread indicator, 2-line preview, 48dp bookmark button.
  - Smooth item placement animation (`animateItem`).
  - Edge-to-edge support with proper padding and insets.
- `app/src/main/java/com/example/notificationvault/ui/details/DetailScreen.kt`:
  - Refined top bar with back navigation and clear icon buttons.
  - Source app header with app icon, title, package name, and "Open app" action.
  - Clean reading typography for captured text with graceful empty-field handling.
  - Snapshot metadata section with event time, capture time, and optional category.
  - Delete dialog with clear destructive confirmation.
- `app/src/main/java/com/example/notificationvault/ui/onboarding/OnboardingScreen.kt`:
  - Polished modern layout with hero branding, honest disclosures, and 48dp+ primary/secondary action buttons.
- `app/src/main/java/com/example/notificationvault/app/NotificationVaultApp.kt`:
  - Refined top bar integration and edge-to-edge scaffolding.

### Launcher Icon Resources
- `app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml` & `ic_launcher_round.xml`
- `app/src/main/res/drawable/ic_launcher_background.xml`
- `app/src/main/res/drawable/ic_launcher_foreground.xml`
- `app/src/main/res/drawable/ic_launcher_monochrome.xml`
- Update `AndroidManifest.xml` to point to `@mipmap/ic_launcher`.

## 3. Preservation of Invariants
- Zero Room database / DAO modifications.
- Zero ViewModels / Repository contract alterations.
- Strict local-first privacy (no telemetry, network, or content logging).
- All existing 63 unit tests must pass without regressions.

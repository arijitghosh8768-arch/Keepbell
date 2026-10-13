package com.example.notificationvault.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import org.w3c.dom.NodeList
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Security and Manifest audit test suite.
 *
 * Implements Phase 4, Task 4.4 / TC-026:
 * - Audits source and effective merged application manifests dynamically (non-brittle paths).
 * - Rejects unapproved permissions: INTERNET, network-state (ACCESS_NETWORK_STATE), SMS, contacts, location, mic, camera.
 * - Verifies VaultNotificationListener service declaration, exported status, and BIND_NOTIFICATION_LISTENER_SERVICE protection.
 * - Verifies android:allowBackup="false" on the effective Application configuration.
 * - Audits exported components to ensure internal components are not exposed without appropriate system permissions.
 * - Audits dependency declarations against unauthorized analytics, telemetry, ads, or crash-reporting SDKs.
 */
class ManifestAndDependencyAuditTest {

    companion object {
        /**
         * Permissions that must NEVER be present in Keepbell's manifest.
         * Per PRD, SECURITY_THREAT_MODEL.md, and PRIVACY_AND_COMPLIANCE.md:
         * Keepbell is an offline-first, local-only notification vault.
         */
        val FORBIDDEN_PERMISSIONS = setOf(
            "android.permission.INTERNET",
            "android.permission.ACCESS_NETWORK_STATE",
            "android.permission.CHANGE_NETWORK_STATE",
            "android.permission.READ_SMS",
            "android.permission.RECEIVE_SMS",
            "android.permission.SEND_SMS",
            "android.permission.READ_CONTACTS",
            "android.permission.WRITE_CONTACTS",
            "android.permission.ACCESS_FINE_LOCATION",
            "android.permission.ACCESS_COARSE_LOCATION",
            "android.permission.ACCESS_BACKGROUND_LOCATION",
            "android.permission.RECORD_AUDIO",
            "android.permission.CAMERA",
            "android.permission.READ_CALL_LOG",
            "android.permission.WRITE_CALL_LOG",
            "android.permission.PROCESS_OUTGOING_CALLS",
            "android.permission.READ_PHONE_STATE",
            "android.permission.BIND_ACCESSIBILITY_SERVICE"
        )

        /**
         * Known analytics, telemetry, advertising, and crash reporting artifact substrings.
         */
        val FORBIDDEN_DEPENDENCY_PATTERNS = listOf(
            "firebase-analytics",
            "firebase-crashlytics",
            "play-services-ads",
            "applovin",
            "unity-ads",
            "mixpanel",
            "amplitude",
            "segment",
            "sentry",
            "bugsnag",
            "flurry",
            "retrofit",
            "okhttp",
            "ktor",
            "volley"
        )
    }

    private fun findManifestFiles(): List<File> {
        val found = mutableListOf<File>()

        // 1. Source manifest (always audited)
        val sourceCandidates = listOf(
            "app/src/main/AndroidManifest.xml",
            "src/main/AndroidManifest.xml"
        )
        val sourceFile = sourceCandidates.map { File(it) }.firstOrNull { it.exists() && it.isFile }
        assertNotNull("Source AndroidManifest.xml must exist", sourceFile)
        found.add(sourceFile!!)

        // 2. Merged manifests
        val mergedCandidates = listOf(
            "app/build/intermediates/merged_manifest/debug/processDebugMainManifest/AndroidManifest.xml",
            "build/intermediates/merged_manifest/debug/processDebugMainManifest/AndroidManifest.xml",
            "app/build/intermediates/merged_manifest/release/processReleaseMainManifest/AndroidManifest.xml",
            "build/intermediates/merged_manifest/release/processReleaseMainManifest/AndroidManifest.xml"
        )

        for (path in mergedCandidates) {
            val file = File(path)
            if (file.exists() && file.isFile) {
                // Ensure we do not audit stale build artifacts that predate source manifest changes
                if (file.lastModified() >= sourceFile.lastModified()) {
                    found.add(file)
                }
            }
        }

        assertTrue("At least one manifest file (source or merged) must be discoverable for audit", found.isNotEmpty())
        return found
    }

    private fun parseXml(file: File): org.w3c.dom.Document {
        val factory = DocumentBuilderFactory.newInstance()
        factory.isNamespaceAware = true
        val builder = factory.newDocumentBuilder()
        return builder.parse(file)
    }

    @Test
    fun manifestAudit_prohibitsUnapprovedPermissionsInAllManifests() {
        val manifestFiles = findManifestFiles()

        for (manifestFile in manifestFiles) {
            val doc = parseXml(manifestFile)
            val usesPermNodes: NodeList = doc.getElementsByTagName("uses-permission")
            val requested = mutableSetOf<String>()

            for (i in 0 until usesPermNodes.length) {
                val element = usesPermNodes.item(i) as Element
                val isRemovalDirective = (0 until element.attributes.length).any { idx ->
                    val attr = element.attributes.item(idx)
                    (attr.nodeName.endsWith(":node") || attr.localName == "node") && attr.nodeValue == "remove"
                }
                // If tools:node="remove", it is an explicit manifest-merger removal directive, not a permission request
                if (isRemovalDirective) {
                    continue
                }
                val name = element.getAttribute("android:name").ifEmpty {
                    element.getAttributeNS("http://schemas.android.com/apk/res/android", "name")
                }
                if (name.isNotEmpty()) {
                    requested.add(name)
                }
            }

            for (forbidden in FORBIDDEN_PERMISSIONS) {
                assertFalse(
                    "Manifest '${manifestFile.name}' (${manifestFile.path}) must not request forbidden permission '$forbidden'",
                    requested.contains(forbidden)
                )
            }
        }
    }

    @Test
    fun manifestAudit_allowBackupIsExplicitlyDisabled() {
        val manifestFiles = findManifestFiles()

        for (manifestFile in manifestFiles) {
            val doc = parseXml(manifestFile)
            val appNodes = doc.getElementsByTagName("application")
            if (appNodes.length > 0) {
                val appElement = appNodes.item(0) as Element
                val allowBackup = appElement.getAttribute("android:allowBackup")
                assertEquals(
                    "Manifest '${manifestFile.name}' application must set android:allowBackup='false'",
                    "false",
                    allowBackup
                )
            }
        }
    }

    @Test
    fun manifestAudit_notificationListenerService_isProperlyProtected() {
        val manifestFiles = findManifestFiles()

        for (manifestFile in manifestFiles) {
            val doc = parseXml(manifestFile)
            val serviceNodes = doc.getElementsByTagName("service")
            var foundVaultListener = false

            for (i in 0 until serviceNodes.length) {
                val serviceElement = serviceNodes.item(i) as Element
                val name = serviceElement.getAttribute("android:name")

                if (name.endsWith("VaultNotificationListener")) {
                    foundVaultListener = true
                    val exported = serviceElement.getAttribute("android:exported")
                    val permission = serviceElement.getAttribute("android:permission")

                    assertEquals(
                        "VaultNotificationListener must be android:exported='true' in '${manifestFile.name}'",
                        "true",
                        exported
                    )
                    assertEquals(
                        "VaultNotificationListener must be protected by BIND_NOTIFICATION_LISTENER_SERVICE in '${manifestFile.name}'",
                        "android.permission.BIND_NOTIFICATION_LISTENER_SERVICE",
                        permission
                    )
                }
            }

            assertTrue("VaultNotificationListener must be declared in '${manifestFile.name}'", foundVaultListener)
        }
    }

    @Test
    fun manifestAudit_exportedComponents_areStrictlyControlled() {
        // Inspect merged manifests and source manifest for exported component compliance
        val manifestFiles = findManifestFiles()

        for (manifestFile in manifestFiles) {
            val doc = parseXml(manifestFile)

            // Activities
            val activityNodes = doc.getElementsByTagName("activity")
            for (i in 0 until activityNodes.length) {
                val elem = activityNodes.item(i) as Element
                val exported = elem.getAttribute("android:exported")
                val name = elem.getAttribute("android:name")
                if (exported == "true") {
                    // App-level activities: only MainActivity may be exported as launcher
                    if (name.startsWith("com.example.notificationvault") || name.startsWith(".")) {
                        assertTrue(
                            "Only MainActivity should be an exported app activity in '${manifestFile.name}' (found: $name)",
                            name.endsWith("MainActivity")
                        )
                    } else {
                        // Library debug activities injected into debug builds (e.g. Compose tooling)
                        assertTrue(
                            "Exported activity '$name' in '${manifestFile.name}' must belong to trusted androidx tooling",
                            name.startsWith("androidx.")
                        )
                    }
                }
            }

            // Services
            val serviceNodes = doc.getElementsByTagName("service")
            for (i in 0 until serviceNodes.length) {
                val elem = serviceNodes.item(i) as Element
                val exported = elem.getAttribute("android:exported")
                val name = elem.getAttribute("android:name")
                if (exported == "true") {
                    val isVaultListener = name.endsWith("VaultNotificationListener")
                    val isSystemJobService = name.endsWith("SystemJobService")
                    assertTrue(
                        "Exported service '$name' in '${manifestFile.name}' must be either VaultNotificationListener or WorkManager SystemJobService",
                        isVaultListener || isSystemJobService
                    )
                    if (isVaultListener) {
                        assertEquals(
                            "VaultNotificationListener must be protected by BIND_NOTIFICATION_LISTENER_SERVICE in '${manifestFile.name}'",
                            "android.permission.BIND_NOTIFICATION_LISTENER_SERVICE",
                            elem.getAttribute("android:permission")
                        )
                    }
                    if (isSystemJobService) {
                        assertEquals(
                            "SystemJobService must be protected by BIND_JOB_SERVICE in '${manifestFile.name}'",
                            "android.permission.BIND_JOB_SERVICE",
                            elem.getAttribute("android:permission")
                        )
                    }
                }
            }

            // Receivers
            val receiverNodes = doc.getElementsByTagName("receiver")
            for (i in 0 until receiverNodes.length) {
                val elem = receiverNodes.item(i) as Element
                val exported = elem.getAttribute("android:exported")
                val name = elem.getAttribute("android:name")
                if (exported == "true") {
                    // App must never export broadcast receivers
                    assertFalse(
                        "App package must never export broadcast receivers in '${manifestFile.name}' (found: $name)",
                        name.startsWith("com.example.notificationvault") || name.startsWith(".")
                    )
                    // Library receivers that are exported must be protected by android.permission.DUMP
                    val permission = elem.getAttribute("android:permission")
                    assertEquals(
                        "Exported library receiver '$name' must be protected by android.permission.DUMP in '${manifestFile.name}'",
                        "android.permission.DUMP",
                        permission
                    )
                }
            }

            // Providers
            val providerNodes = doc.getElementsByTagName("provider")
            for (i in 0 until providerNodes.length) {
                val elem = providerNodes.item(i) as Element
                val exported = elem.getAttribute("android:exported")
                val name = elem.getAttribute("android:name")
                assertEquals(
                    "Content provider '$name' in '${manifestFile.name}' must not be exported",
                    "false",
                    exported
                )
            }
        }
    }

    @Test
    fun dependencyAudit_noUnauthorizedTelemetryOrAnalyticsDeclared() {
        // Inspect version catalog file: gradle/libs.versions.toml
        val tomlFile = listOf(
            File("gradle/libs.versions.toml"),
            File("../gradle/libs.versions.toml")
        ).firstOrNull { it.exists() }

        assertNotNull("Version catalog 'libs.versions.toml' must exist", tomlFile)
        val tomlContent = tomlFile!!.readText().lowercase()

        for (forbidden in FORBIDDEN_DEPENDENCY_PATTERNS) {
            assertFalse(
                "Version catalog must not declare forbidden telemetry/analytics/networking dependency '$forbidden'",
                tomlContent.contains(forbidden)
            )
        }

        // Inspect app/build.gradle.kts
        val buildGradleFile = listOf(
            File("app/build.gradle.kts"),
            File("build.gradle.kts")
        ).firstOrNull { it.exists() }

        assertNotNull("app/build.gradle.kts must exist", buildGradleFile)
        val buildContent = buildGradleFile!!.readText().lowercase()

        for (forbidden in FORBIDDEN_DEPENDENCY_PATTERNS) {
            assertFalse(
                "build.gradle.kts must not declare forbidden dependency '$forbidden'",
                buildContent.contains(forbidden)
            )
        }
    }
}

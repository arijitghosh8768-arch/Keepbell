package com.example.notificationvault.domain.capture

import com.example.notificationvault.domain.model.ArchivedNotification
import com.example.notificationvault.domain.repository.NotificationRepository
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.security.MessageDigest
import java.util.UUID

/**
 * Central processing pipeline for notification events.
 *
 * Enforces:
 * 1. Global capture and per-package exclusion checks before database operations.
 * 2. Strict field length limits and whitespace normalization (Threat T-05).
 * 3. Best-effort update of existing notifications when matching nonblank source keys exist (DATABASE.md §5).
 * 4. Deterministic SHA-256 fingerprinting with a bounded duplicate window when source keys are missing.
 * 5. Preservation of existing records, bookmark states, and read flags (FR-05, FR-06).
 */
class CaptureProcessor(
    private val notificationRepository: NotificationRepository,
    private val captureSettingsRepository: CaptureSettingsRepository,
    private val currentTimeMillisProvider: () -> Long = { System.currentTimeMillis() },
    private val duplicateWindowMs: Long = DEFAULT_DUPLICATE_WINDOW_MS
) {

    // Process-level mutex serializing concurrent callbacks within the app process.
    // Note: Cross-process database concurrency is not claimed to be atomic.
    private val processingMutex = Mutex()

    /**
     * Processes an extracted [NotificationCandidate].
     *
     * Returns the persisted or updated [ArchivedNotification], or `null` if the candidate
     * was ignored due to global/per-app settings or invalid content.
     */
    suspend fun process(candidate: NotificationCandidate): ArchivedNotification? = processingMutex.withLock {
        val rawPackageName = candidate.packageName.trim()
        if (rawPackageName.isEmpty()) return null

        val normPackageName = rawPackageName.take(MAX_PACKAGE_NAME_LENGTH)

        // 1. Check global capture toggle before any database access (TC-010, PRD FR-11)
        if (!captureSettingsRepository.getGlobalCaptureEnabled()) {
            return null
        }

        // 2. Check per-package exclusion list before any database access (TC-009, PRD FR-11, US-13)
        if (captureSettingsRepository.isPackageExcluded(normPackageName)) {
            return null
        }

        // 3. Normalize fields and enforce length caps (Threat T-05, TC-027)
        val normAppLabel = candidate.appLabel?.trim()?.take(MAX_APP_LABEL_LENGTH)?.takeIf { it.isNotBlank() }
        val normTitle = candidate.title?.trim()?.take(MAX_TITLE_LENGTH)?.takeIf { it.isNotBlank() }
        val normBody = candidate.body?.trim()?.take(MAX_BODY_LENGTH)?.takeIf { it.isNotBlank() }
        val normCategory = candidate.category?.trim()?.take(MAX_CATEGORY_LENGTH)?.takeIf { it.isNotBlank() }
        val sourceKey = candidate.sourceNotificationKey?.trim()?.takeIf { it.isNotBlank() }

        val capturedAtMs = currentTimeMillisProvider()
        val eventTimeMs = candidate.eventTimeMs

        // 4. Compute deterministic SHA-256 fingerprint
        val fingerprint = computeFingerprint(normPackageName, normTitle, normBody)

        // 5. Apply deduplication and update policy (DATABASE.md §5)
        return if (sourceKey != null) {
            handleSourceKeyCapture(
                sourceKey = sourceKey,
                normPackageName = normPackageName,
                normAppLabel = normAppLabel,
                normTitle = normTitle,
                normBody = normBody,
                normCategory = normCategory,
                eventTimeMs = eventTimeMs,
                capturedAtMs = capturedAtMs,
                fingerprint = fingerprint
            )
        } else {
            handleFingerprintCapture(
                normPackageName = normPackageName,
                normAppLabel = normAppLabel,
                normTitle = normTitle,
                normBody = normBody,
                normCategory = normCategory,
                eventTimeMs = eventTimeMs,
                capturedAtMs = capturedAtMs,
                fingerprint = fingerprint
            )
        }
    }

    private suspend fun handleSourceKeyCapture(
        sourceKey: String,
        normPackageName: String,
        normAppLabel: String?,
        normTitle: String?,
        normBody: String?,
        normCategory: String?,
        eventTimeMs: Long,
        capturedAtMs: Long,
        fingerprint: String
    ): ArchivedNotification {
        val existing = notificationRepository.findBySourceKey(sourceKey)
        return if (existing != null) {
            // Update existing record in-place, preserving its ID, bookmark, and read state (TC-006, TC-008)
            val updated = existing.copy(
                appLabel = normAppLabel ?: existing.appLabel,
                title = normTitle,
                body = normBody,
                eventTimeMs = eventTimeMs,
                capturedAtMs = capturedAtMs,
                category = normCategory ?: existing.category,
                dedupeFingerprint = fingerprint
            )
            notificationRepository.updateNotification(updated)
            updated
        } else {
            // New record under this source key
            val newNotification = ArchivedNotification(
                id = UUID.randomUUID().toString(),
                packageName = normPackageName,
                appLabel = normAppLabel,
                title = normTitle,
                body = normBody,
                eventTimeMs = eventTimeMs,
                capturedAtMs = capturedAtMs,
                isBookmarked = false,
                isRead = false,
                category = normCategory,
                sourceNotificationKey = sourceKey,
                dedupeFingerprint = fingerprint
            )
            notificationRepository.saveNotification(newNotification)
            newNotification
        }
    }

    private suspend fun handleFingerprintCapture(
        normPackageName: String,
        normAppLabel: String?,
        normTitle: String?,
        normBody: String?,
        normCategory: String?,
        eventTimeMs: Long,
        capturedAtMs: Long,
        fingerprint: String
    ): ArchivedNotification {
        val existing = notificationRepository.findByFingerprint(fingerprint)
        val isWithinDuplicateWindow = existing != null &&
                (capturedAtMs - existing.capturedAtMs) in 0..duplicateWindowMs

        return if (existing != null && isWithinDuplicateWindow) {
            // Rapid burst duplicate: update existing record timestamps without creating a duplicate row (TC-008)
            val updated = existing.copy(
                eventTimeMs = eventTimeMs,
                capturedAtMs = capturedAtMs
            )
            notificationRepository.updateNotification(updated)
            updated
        } else {
            // Outside bounded window or no match: legitimate distinct alert (TC-007)
            val newNotification = ArchivedNotification(
                id = UUID.randomUUID().toString(),
                packageName = normPackageName,
                appLabel = normAppLabel,
                title = normTitle,
                body = normBody,
                eventTimeMs = eventTimeMs,
                capturedAtMs = capturedAtMs,
                isBookmarked = false,
                isRead = false,
                category = normCategory,
                sourceNotificationKey = null,
                dedupeFingerprint = fingerprint
            )
            notificationRepository.saveNotification(newNotification)
            newNotification
        }
    }

    companion object {
        const val MAX_PACKAGE_NAME_LENGTH = 256
        const val MAX_TITLE_LENGTH = 256
        const val MAX_BODY_LENGTH = 4096
        const val MAX_APP_LABEL_LENGTH = 128
        const val MAX_CATEGORY_LENGTH = 64
        const val DEFAULT_DUPLICATE_WINDOW_MS = 5000L

        /**
         * Computes a deterministic SHA-256 fingerprint from package, title, and body.
         */
        fun computeFingerprint(packageName: String, title: String?, body: String?): String {
            val input = "$packageName\u0000${title.orEmpty()}\u0000${body.orEmpty()}"
            val digest = MessageDigest.getInstance("SHA-256")
            val hashBytes = digest.digest(input.toByteArray(Charsets.UTF_8))
            return hashBytes.joinToString("") { "%02x".format(it) }
        }
    }
}

package com.example.notificationvault.data.local

import com.example.notificationvault.domain.model.ArchivedNotification
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationMapperTest {

    @Test
    fun entityToDomain_mapsAllFieldsAccurately() {
        val entity = NotificationEntity(
            id = "uuid-123",
            packageName = "com.example.app",
            appLabel = "Example App",
            title = "Test Title",
            body = "Test Body",
            eventTimeMs = 1000L,
            capturedAtMs = 1005L,
            isBookmarked = true,
            isRead = true,
            category = "msg",
            expiresAtMs = 5000L,
            dedupeFingerprint = "fp_123",
            sourceNotificationKey = "key_123"
        )

        val domain = entity.toDomain()

        assertEquals("uuid-123", domain.id)
        assertEquals("com.example.app", domain.packageName)
        assertEquals("Example App", domain.appLabel)
        assertEquals("Test Title", domain.title)
        assertEquals("Test Body", domain.body)
        assertEquals(1000L, domain.eventTimeMs)
        assertEquals(1005L, domain.capturedAtMs)
        assertTrue(domain.isBookmarked)
        assertTrue(domain.isRead)
        assertEquals("msg", domain.category)
        assertEquals(5000L, domain.expiresAtMs)
        assertEquals("fp_123", domain.dedupeFingerprint)
        assertEquals("key_123", domain.sourceNotificationKey)
    }

    @Test
    fun domainToEntity_mapsAllFieldsAccurately() {
        val domain = ArchivedNotification(
            id = "uuid-456",
            packageName = "com.sample.mail",
            appLabel = "Sample Mail",
            title = "Welcome",
            body = "Thanks for joining",
            eventTimeMs = 2000L,
            capturedAtMs = 2010L,
            isBookmarked = false,
            isRead = false,
            category = "promo",
            expiresAtMs = 8000L,
            dedupeFingerprint = "fp_456",
            sourceNotificationKey = "key_456"
        )

        val entity = domain.toEntity()

        assertEquals("uuid-456", entity.id)
        assertEquals("com.sample.mail", entity.packageName)
        assertEquals("Sample Mail", entity.appLabel)
        assertEquals("Welcome", entity.title)
        assertEquals("Thanks for joining", entity.body)
        assertEquals(2000L, entity.eventTimeMs)
        assertEquals(2010L, entity.capturedAtMs)
        assertFalse(entity.isBookmarked)
        assertFalse(entity.isRead)
        assertEquals("promo", entity.category)
        assertEquals(8000L, entity.expiresAtMs)
        assertEquals("fp_456", entity.dedupeFingerprint)
        assertEquals("key_456", entity.sourceNotificationKey)
    }

    @Test
    fun displayAppLabel_fallsBackToPackageName_whenLabelIsMissingOrBlank() {
        val withNullLabel = ArchivedNotification(
            packageName = "org.telegram.messenger",
            appLabel = null,
            eventTimeMs = 1L,
            capturedAtMs = 1L
        )
        val withBlankLabel = ArchivedNotification(
            packageName = "org.telegram.messenger",
            appLabel = "   ",
            eventTimeMs = 1L,
            capturedAtMs = 1L
        )
        val withValidLabel = ArchivedNotification(
            packageName = "org.telegram.messenger",
            appLabel = "Telegram",
            eventTimeMs = 1L,
            capturedAtMs = 1L
        )

        assertEquals("org.telegram.messenger", withNullLabel.displayAppLabel)
        assertEquals("org.telegram.messenger", withBlankLabel.displayAppLabel)
        assertEquals("Telegram", withValidLabel.displayAppLabel)
    }

    @Test
    fun displayBody_fallsBackToPlaceholder_whenBodyIsMissingOrBlank() {
        val withNullBody = ArchivedNotification(
            packageName = "com.app",
            body = null,
            eventTimeMs = 1L,
            capturedAtMs = 1L
        )
        val withBlankBody = ArchivedNotification(
            packageName = "com.app",
            body = "",
            eventTimeMs = 1L,
            capturedAtMs = 1L
        )
        val withContentBody = ArchivedNotification(
            packageName = "com.app",
            body = "Hello there",
            eventTimeMs = 1L,
            capturedAtMs = 1L
        )

        assertEquals("No preview available", withNullBody.displayBody)
        assertEquals("No preview available", withBlankBody.displayBody)
        assertEquals("Hello there", withContentBody.displayBody)
    }
}

package com.example.notificationvault.data.local

import androidx.room.TypeConverter
import java.util.Date

/**
 * Room type converters for platform types when required.
 */
class Converters {
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }
}

package com.example.notificationvault.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [NotificationEntity::class],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class VaultDatabase : RoomDatabase() {

    abstract fun notificationDao(): NotificationDao

    companion object {
        private const val DATABASE_NAME = "notification_vault.db"

        @Volatile
        private var INSTANCE: VaultDatabase? = null

        fun getInstance(context: Context): VaultDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    VaultDatabase::class.java,
                    DATABASE_NAME
                ).build().also { INSTANCE = it }
            }
        }

        fun createInMemoryDatabase(context: Context): VaultDatabase {
            return Room.inMemoryDatabaseBuilder(
                context.applicationContext,
                VaultDatabase::class.java
            ).allowMainThreadQueries().build()
        }
    }
}

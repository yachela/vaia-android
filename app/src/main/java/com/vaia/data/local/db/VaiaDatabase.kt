package com.vaia.data.local.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [TripEntity::class, ActivityEntity::class, PackingListEntity::class, PackingItemEntity::class, DocumentEntity::class, ChecklistItemEntity::class],
    version = 6,
    exportSchema = false
)
abstract class VaiaDatabase : RoomDatabase() {

    abstract fun tripDao(): TripDao
    abstract fun activityDao(): ActivityDao
    abstract fun packingDao(): PackingDao
    abstract fun documentDao(): DocumentDao

    companion object {
        @Volatile private var INSTANCE: VaiaDatabase? = null

        fun getInstance(context: Context): VaiaDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    VaiaDatabase::class.java,
                    "vaia_db"
                )
                .fallbackToDestructiveMigration() // For development, recreate DB on schema changes
                .build().also { INSTANCE = it }
            }
        }
    }
}

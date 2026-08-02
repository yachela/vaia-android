package com.vaia.data.local.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [TripEntity::class, TripCurrencyEntity::class, ActivityEntity::class, PackingListEntity::class, PackingItemEntity::class, DocumentEntity::class, ChecklistItemEntity::class, ExpenseEntity::class],
    version = 8,
    exportSchema = false
)
abstract class VaiaDatabase : RoomDatabase() {

    abstract fun tripDao(): TripDao
    abstract fun tripCurrencyDao(): TripCurrencyDao
    abstract fun activityDao(): ActivityDao
    abstract fun packingDao(): PackingDao
    abstract fun documentDao(): DocumentDao
    abstract fun expenseDao(): ExpenseDao

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

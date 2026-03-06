package com.vaia.data.local.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [TripEntity::class, ActivityEntity::class],
    version = 1,
    exportSchema = false
)
abstract class VaiaDatabase : RoomDatabase() {

    abstract fun tripDao(): TripDao
    abstract fun activityDao(): ActivityDao

    companion object {
        @Volatile private var INSTANCE: VaiaDatabase? = null

        fun getInstance(context: Context): VaiaDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    VaiaDatabase::class.java,
                    "vaia_db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}

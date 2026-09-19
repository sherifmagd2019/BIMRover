package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [StakedRecordEntity::class, ProjectCalibrationEntity::class],
    version = 1,
    exportSchema = false
)
abstract class StakeoutDatabase : RoomDatabase() {

    abstract fun stakeoutDao(): StakeoutDao

    companion object {
        @Volatile
        private var instance: StakeoutDatabase? = null

        fun getInstance(context: Context): StakeoutDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    StakeoutDatabase::class.java,
                    "bim_surveyor_stakeout.db"
                ).fallbackToDestructiveMigration(dropAllTables = true).build().also { instance = it }
            }
        }
    }
}

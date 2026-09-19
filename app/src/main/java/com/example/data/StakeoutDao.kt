package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface StakeoutDao {

    @Query("SELECT * FROM staked_records ORDER BY timestamp DESC")
    fun getAllStakedRecords(): Flow<List<StakedRecordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStakedRecord(record: StakedRecordEntity): Long

    @Query("DELETE FROM staked_records WHERE id = :recordId")
    suspend fun deleteStakedRecord(recordId: Long)

    @Query("DELETE FROM staked_records")
    suspend fun clearAllRecords()

    @Query("SELECT * FROM project_calibration WHERE siteKey = :key LIMIT 1")
    suspend fun getCalibration(key: String = "default_site"): ProjectCalibrationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveCalibration(calibration: ProjectCalibrationEntity)
}

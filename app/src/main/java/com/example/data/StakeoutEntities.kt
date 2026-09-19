package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Persisted record of a physically surveyed/staked point on the job site.
 */
@Entity(tableName = "staked_records")
data class StakedRecordEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val pointName: String,
    val pointCategory: String,
    val targetLat: Double,
    val targetLng: Double,
    val targetElev: Double,
    val measuredLat: Double,
    val measuredLng: Double,
    val measuredElev: Double,
    val deltaNorthMeters: Double,
    val deltaEastMeters: Double,
    val deltaElevMeters: Double,
    val totalHorizontalErrorMeters: Double,
    val isWithinTolerance: Boolean,
    val surveyorName: String = "Field Engineer 1",
    val notes: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Persisted Project Base Point calibration.
 */
@Entity(tableName = "project_calibration")
data class ProjectCalibrationEntity(
    @PrimaryKey
    val siteKey: String = "default_site",
    val siteName: String,
    val anchorLat: Double,
    val anchorLng: Double,
    val anchorElevation: Double,
    val trueNorthOffsetDegrees: Double,
    val unitLabel: String = "METERS",
    val lastModifiedTimestamp: Long = System.currentTimeMillis()
)

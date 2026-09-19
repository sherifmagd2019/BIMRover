package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ProjectCalibrationEntity
import com.example.data.StakedRecordEntity
import com.example.data.StakeoutDatabase
import com.example.gis.GpxExportMode
import com.example.gis.GpxExportOptions
import com.example.gis.GpxExporter
import com.example.gis.OfflineSatelliteTileManager
import com.example.location.FieldLocationTracker
import com.example.math.GeodeticCoordinateTransformer
import com.example.model.GnssTelemetry
import com.example.model.MapEngine
import com.example.model.MapLayerMode
import com.example.model.OfflineSatelliteCacheStatus
import com.example.model.ProjectAnchor
import com.example.model.RevitCadGridLine
import com.example.model.RevitPoint
import com.example.model.RevitStructuralPolygon
import com.example.model.StakeoutGuidance
import com.example.model.SurveyUnit
import com.example.model.TransformedStakeoutPoint
import com.example.monetization.RevenueCatManager
import com.example.parser.RevitPointParser
import com.example.speech.StakeoutSpeechManager
import com.example.speech.StakeoutVoiceMode
import com.example.speech.StakeoutVoiceSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.math.sqrt

class BimSurveyorViewModel(application: Application) : AndroidViewModel(application) {

    val revenueCatManager = RevenueCatManager.getInstance(application)
    val locationTracker = FieldLocationTracker(application)
    private val database = StakeoutDatabase.getInstance(application)
    private val stakeoutDao = database.stakeoutDao()

    // 1. Project Calibration & Points State
    private val _projectAnchor = MutableStateFlow(RevitPointParser.getSkylineTowerAnchor())
    val projectAnchor: StateFlow<ProjectAnchor> = _projectAnchor.asStateFlow()

    private val _revitPoints = MutableStateFlow(RevitPointParser.getSkylineTowerSamplePoints())
    val revitPoints: StateFlow<List<RevitPoint>> = _revitPoints.asStateFlow()

    private val _selectedPointIndex = MutableStateFlow(0)
    val selectedPointIndex: StateFlow<Int> = _selectedPointIndex.asStateFlow()

    // High Sunlight Outdoor Glaze Mode
    private val _isSunGlazeMode = MutableStateFlow(false)
    val isSunGlazeMode: StateFlow<Boolean> = _isSunGlazeMode.asStateFlow()

    // Surveyor Staking Tolerance
    private val _toleranceMeters = MutableStateFlow(0.020) // 20mm default
    val toleranceMeters: StateFlow<Double> = _toleranceMeters.asStateFlow()

    // Staked Records Flow from Room
    val stakedRecords: StateFlow<List<StakedRecordEntity>> = stakeoutDao.getAllStakedRecords()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Map & GIS Satellite Display State
    val tileManager = OfflineSatelliteTileManager.getInstance(application)
    val offlineCacheStatus: StateFlow<OfflineSatelliteCacheStatus> = tileManager.cacheStatus

    private val _mapLayerMode = MutableStateFlow(MapLayerMode.SATELLITE)
    val mapLayerMode: StateFlow<MapLayerMode> = _mapLayerMode.asStateFlow()

    private val _mapEngine = MutableStateFlow(MapEngine.OFFLINE_GIS)
    val mapEngine: StateFlow<MapEngine> = _mapEngine.asStateFlow()

    // 2. Pure Geodetic Math Transformation: Recomputes transformed points whenever anchor or points change
    val transformedPoints: StateFlow<List<TransformedStakeoutPoint>> = combine(
        _projectAnchor,
        _revitPoints
    ) { anchor, points ->
        points.map { pt ->
            GeodeticCoordinateTransformer.transformPoint(pt, anchor)
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // 2b. Structural CAD Grid Lines & Footprints
    val cadGridLines: StateFlow<List<RevitCadGridLine>> = combine(
        _projectAnchor,
        _revitPoints
    ) { anchor, points ->
        GeodeticCoordinateTransformer.generateRevitCadGridLines(anchor, points)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val structuralFootprints: StateFlow<List<RevitStructuralPolygon>> = combine(
        _projectAnchor,
        _revitPoints
    ) { anchor, points ->
        GeodeticCoordinateTransformer.generateStructuralFootprints(anchor, points)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // 3. Real-Time Dynamic Guidance: Recomputes relative distance, directional arrows, and Cut/Fill
    val activeGuidance: StateFlow<StakeoutGuidance?> = combine(
        transformedPoints,
        _selectedPointIndex,
        locationTracker.telemetry,
        _toleranceMeters
    ) { transformedList, index, telem, tol ->
        if (transformedList.isEmpty() || index !in transformedList.indices) {
            null
        } else {
            val target = transformedList[index]
            GeodeticCoordinateTransformer.calculateStakeoutGuidance(
                target = target,
                currentLat = telem.latitude,
                currentLng = telem.longitude,
                currentElevationMeters = telem.altitudeMeters,
                deviceHeadingDegrees = telem.deviceHeadingDegrees,
                horizontalToleranceMeters = tol
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    init {
        // Start GNSS tracking
        locationTracker.startTracking()

        // Sync initial position to anchor for seamless first-launch demonstration
        val initialAnchor = _projectAnchor.value
        locationTracker.setPosition(
            initialAnchor.latitude,
            initialAnchor.longitude,
            initialAnchor.elevationMeters
        )

        // Load any persisted calibration from local database
        viewModelScope.launch {
            val savedCal = stakeoutDao.getCalibration()
            if (savedCal != null) {
                _projectAnchor.value = ProjectAnchor(
                    id = savedCal.siteKey,
                    siteName = savedCal.siteName,
                    latitude = savedCal.anchorLat,
                    longitude = savedCal.anchorLng,
                    elevationMeters = savedCal.anchorElevation,
                    trueNorthOffsetDegrees = savedCal.trueNorthOffsetDegrees,
                    unit = try { SurveyUnit.valueOf(savedCal.unitLabel) } catch (_: Exception) { SurveyUnit.METERS }
                )
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        locationTracker.stopTracking()
    }

    // --- Actions ---

    fun selectPoint(index: Int) {
        if (index in _revitPoints.value.indices) {
            _selectedPointIndex.value = index
        }
    }

    fun nextPoint() {
        val count = _revitPoints.value.size
        if (count > 0) {
            _selectedPointIndex.value = (_selectedPointIndex.value + 1) % count
        }
    }

    fun previousPoint() {
        val count = _revitPoints.value.size
        if (count > 0) {
            _selectedPointIndex.value = (_selectedPointIndex.value - 1 + count) % count
        }
    }

    fun toggleSunGlazeMode() {
        _isSunGlazeMode.value = !_isSunGlazeMode.value
    }

    fun setTolerance(toleranceMeters: Double) {
        _toleranceMeters.value = toleranceMeters
    }

    fun updateAnchor(
        siteName: String,
        latitude: Double,
        longitude: Double,
        elevation: Double,
        trueNorthDegrees: Double,
        unit: SurveyUnit
    ) {
        val newAnchor = ProjectAnchor(
            id = "user_configured_anchor",
            siteName = siteName,
            latitude = latitude,
            longitude = longitude,
            elevationMeters = elevation,
            trueNorthOffsetDegrees = trueNorthDegrees,
            unit = unit
        )
        _projectAnchor.value = newAnchor

        // Persist to Room
        viewModelScope.launch {
            stakeoutDao.saveCalibration(
                ProjectCalibrationEntity(
                    siteKey = "default_site",
                    siteName = siteName,
                    anchorLat = latitude,
                    anchorLng = longitude,
                    anchorElevation = elevation,
                    trueNorthOffsetDegrees = trueNorthDegrees,
                    unitLabel = unit.name
                )
            )
        }
    }

    fun calibrateAnchorWithCurrentGps() {
        val telem = locationTracker.telemetry.value
        updateAnchor(
            siteName = _projectAnchor.value.siteName,
            latitude = telem.latitude,
            longitude = telem.longitude,
            elevation = telem.altitudeMeters,
            trueNorthDegrees = _projectAnchor.value.trueNorthOffsetDegrees,
            unit = _projectAnchor.value.unit
        )
    }

    fun importCsvPoints(csvString: String): Int {
        val parsed = RevitPointParser.parseCsv(csvString)
        if (parsed.isNotEmpty()) {
            _revitPoints.value = parsed
            _selectedPointIndex.value = 0
        }
        return parsed.size
    }

    fun importJsonPoints(jsonString: String): Int {
        val parsed = RevitPointParser.parseJson(jsonString)
        if (parsed.isNotEmpty()) {
            _revitPoints.value = parsed
            _selectedPointIndex.value = 0
        }
        return parsed.size
    }

    fun loadPreset(preset: String) {
        when (preset) {
            "skyline" -> {
                _projectAnchor.value = RevitPointParser.getSkylineTowerAnchor()
                _revitPoints.value = RevitPointParser.getSkylineTowerSamplePoints()
                _selectedPointIndex.value = 0
            }
            "bridge" -> {
                _projectAnchor.value = RevitPointParser.getBridgeAbutmentAnchor()
                _revitPoints.value = RevitPointParser.getBridgeAbutmentSamplePoints()
                _selectedPointIndex.value = 0
            }
        }
    }

    fun storeCurrentStakedPoint(notes: String = "") {
        val guidance = activeGuidance.value ?: return
        val telem = locationTracker.telemetry.value
        val target = guidance.targetPoint

        val deltaN = guidance.deltaNorthMeters
        val deltaE = guidance.deltaEastMeters
        val deltaZ = guidance.deltaElevationMeters
        val horizError = sqrt(deltaN * deltaN + deltaE * deltaE)

        val record = StakedRecordEntity(
            pointName = target.point.name,
            pointCategory = target.point.category,
            targetLat = target.targetLatitude,
            targetLng = target.targetLongitude,
            targetElev = target.targetElevationMeters,
            measuredLat = telem.latitude,
            measuredLng = telem.longitude,
            measuredElev = telem.altitudeMeters,
            deltaNorthMeters = deltaN,
            deltaEastMeters = deltaE,
            deltaElevMeters = deltaZ,
            totalHorizontalErrorMeters = horizError,
            isWithinTolerance = guidance.isInTolerance,
            notes = notes,
            timestamp = System.currentTimeMillis()
        )

        viewModelScope.launch {
            stakeoutDao.insertStakedRecord(record)
        }
    }

    fun deleteStakedRecord(id: Long) {
        viewModelScope.launch {
            stakeoutDao.deleteStakedRecord(id)
        }
    }

    fun clearAllStakedRecords() {
        viewModelScope.launch {
            stakeoutDao.clearAllRecords()
        }
    }

    fun exportStakedRecordsCsv(): String {
        val list = stakedRecords.value
        val sb = StringBuilder()
        sb.append("Point_Name,Category,Target_Lat,Target_Lng,Target_Elev,Measured_Lat,Measured_Lng,Measured_Elev,Delta_North_m,Delta_East_m,Delta_Elev_m,Horiz_Error_m,In_Tolerance,Timestamp,Notes\n")
        for (r in list) {
            sb.append("${r.pointName},${r.pointCategory},${"%.7f".format(r.targetLat)},${"%.7f".format(r.targetLng)},${"%.4f".format(r.targetElev)},")
            sb.append("${"%.7f".format(r.measuredLat)},${"%.7f".format(r.measuredLng)},${"%.4f".format(r.measuredElev)},")
            sb.append("${"%.4f".format(r.deltaNorthMeters)},${"%.4f".format(r.deltaEastMeters)},${"%.4f".format(r.deltaElevMeters)},")
            sb.append("${"%.4f".format(r.totalHorizontalErrorMeters)},${r.isWithinTolerance},${r.timestamp},\"${r.notes}\"\n")
        }
        return sb.toString()
    }

    fun exportStakedRecordsGpx(options: GpxExportOptions = GpxExportOptions()): String {
        return GpxExporter.generateGpx(
            records = stakedRecords.value,
            options = options.copy(projectName = _projectAnchor.value.siteName.ifBlank { "BIM Stakeout As-Built Survey" })
        )
    }

    fun exportSingleRecordGpx(record: StakedRecordEntity): String {
        return GpxExporter.generateSinglePointGpx(record)
    }

    fun setMapLayerMode(mode: MapLayerMode) {
        _mapLayerMode.value = mode
    }

    fun setMapEngine(engine: MapEngine) {
        _mapEngine.value = engine
    }

    fun selectPointById(pointId: String) {
        val index = _revitPoints.value.indexOfFirst { it.id == pointId }
        if (index != -1) {
            _selectedPointIndex.value = index
        }
    }

    fun preCacheSiteSatelliteTiles() {
        val pts = transformedPoints.value
        val anchor = _projectAnchor.value
        val lats = if (pts.isNotEmpty()) pts.map { it.targetLatitude } + anchor.latitude else listOf(anchor.latitude)
        val lngs = if (pts.isNotEmpty()) pts.map { it.targetLongitude } + anchor.longitude else listOf(anchor.longitude)

        val minLat = lats.minOrNull() ?: anchor.latitude - 0.002
        val maxLat = lats.maxOrNull() ?: anchor.latitude + 0.002
        val minLng = lngs.minOrNull() ?: anchor.longitude - 0.002
        val maxLng = lngs.maxOrNull() ?: anchor.longitude + 0.002

        viewModelScope.launch {
            tileManager.preCacheSiteTiles(
                minLat = minLat - 0.001,
                maxLat = maxLat + 0.001,
                minLng = minLng - 0.001,
                maxLng = maxLng + 0.001,
                minZoom = 16,
                maxZoom = 18
            )
        }
    }

    fun clearSatelliteCache() {
        viewModelScope.launch {
            tileManager.clearCache()
        }
    }
}

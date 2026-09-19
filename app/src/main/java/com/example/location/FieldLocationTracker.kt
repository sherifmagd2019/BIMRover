package com.example.location

import android.annotation.SuppressLint
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.os.Looper
import com.example.model.GnssTelemetry
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Real-time GNSS and Compass Azimuth hardware collector for field surveying.
 */
class FieldLocationTracker(private val context: Context) : SensorEventListener {

    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    private val sensorManager: SensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    private val rotationSensor: Sensor? =
        sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

    private val _telemetry = MutableStateFlow(
        GnssTelemetry(
            latitude = 37.774929,
            longitude = -122.419416,
            altitudeMeters = 35.420,
            horizontalAccuracyMeters = 0.015f,
            verticalAccuracyMeters = 0.020f,
            deviceHeadingDegrees = 0.0f,
            satellitesCount = 22,
            isRtkFixed = true,
            isSimulated = true
        )
    )
    val telemetry: StateFlow<GnssTelemetry> = _telemetry.asStateFlow()

    private var isTrackingLive = false
    private var isSimulatedMode = false

    private val rotationMatrix = FloatArray(9)
    private val orientationAngles = FloatArray(3)

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            val location: Location? = result.lastLocation
            if (location != null && !isSimulatedMode) {
                _telemetry.value = _telemetry.value.copy(
                    latitude = location.latitude,
                    longitude = location.longitude,
                    altitudeMeters = location.altitude,
                    horizontalAccuracyMeters = if (location.hasAccuracy()) location.accuracy else 0.05f,
                    verticalAccuracyMeters = if (location.hasVerticalAccuracy()) location.verticalAccuracyMeters else 0.08f,
                    isRtkFixed = (location.accuracy < 0.10f),
                    isSimulated = false,
                    lastUpdateTimestamp = System.currentTimeMillis()
                )
            }
        }
    }

    /**
     * Starts continuous high-precision GPS and compass tracking.
     */
    @SuppressLint("MissingPermission")
    fun startTracking() {
        if (isTrackingLive) return
        isTrackingLive = true

        try {
            val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 500L)
                .setMinUpdateIntervalMillis(250L)
                .setMinUpdateDistanceMeters(0.01f) // 1 cm threshold
                .build()

            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        } catch (_: SecurityException) {
            // Gracefully handle missing runtime permission
            isSimulatedMode = true
        }

        rotationSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    /**
     * Stops continuous updates to conserve battery.
     */
    fun stopTracking() {
        isTrackingLive = false
        fusedLocationClient.removeLocationUpdates(locationCallback)
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return
        if (event.sensor.type == Sensor.TYPE_ROTATION_VECTOR) {
            SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
            SensorManager.getOrientation(rotationMatrix, orientationAngles)
            // Azimuth in radians around Z axis: -PI to +PI
            val azimuthRad = orientationAngles[0]
            var azimuthDeg = (azimuthRad * 180.0 / PI).toFloat()
            if (azimuthDeg < 0) azimuthDeg += 360f

            _telemetry.value = _telemetry.value.copy(
                deviceHeadingDegrees = azimuthDeg
            )
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    fun setSimulationMode(enabled: Boolean) {
        isSimulatedMode = enabled
        _telemetry.value = _telemetry.value.copy(isSimulated = enabled)
    }

    /**
     * Sets current position explicitly (used in simulation or when resetting to anchor).
     */
    fun setPosition(lat: Double, lng: Double, elevation: Double) {
        _telemetry.value = _telemetry.value.copy(
            latitude = lat,
            longitude = lng,
            altitudeMeters = elevation,
            lastUpdateTimestamp = System.currentTimeMillis()
        )
    }

    /**
     * Simulates physical surveyor steps in metric North/East offsets.
     */
    fun stepSimulatedPosition(deltaNorthMeters: Double, deltaEastMeters: Double, deltaElevMeters: Double = 0.0) {
        val current = _telemetry.value
        val latRad = current.latitude * PI / 180.0

        // Approximate meters to degrees for local step
        val dLat = deltaNorthMeters / 111132.92
        val dLng = deltaEastMeters / (111412.84 * cos(latRad))

        _telemetry.value = current.copy(
            latitude = current.latitude + dLat,
            longitude = current.longitude + dLng,
            altitudeMeters = current.altitudeMeters + deltaElevMeters,
            isSimulated = true,
            lastUpdateTimestamp = System.currentTimeMillis()
        )
    }

    /**
     * Rotates simulated device heading by degrees.
     */
    fun rotateHeading(deltaDegrees: Float) {
        val current = _telemetry.value.deviceHeadingDegrees
        var updated = (current + deltaDegrees) % 360f
        if (updated < 0f) updated += 360f
        _telemetry.value = _telemetry.value.copy(deviceHeadingDegrees = updated)
    }
}

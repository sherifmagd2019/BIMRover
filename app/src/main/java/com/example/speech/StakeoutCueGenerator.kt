package com.example.speech

import com.example.model.StakeoutGuidance
import kotlin.math.abs
import kotlin.math.roundToInt

enum class StakeoutVoiceMode(val label: String, val description: String) {
    FULL_CUES(
        label = "Full Guidance",
        description = "Proximity status, directional cues, distance checkpoints, and stop alerts"
    ),
    PROXIMITY_AND_STOP(
        label = "Proximity & Stop",
        description = "'Moving closer', 'Approaching', 'Moving away', and 'Stop, point reached'"
    ),
    DISTANCE_ONLY(
        label = "Distance & Stop",
        description = "Spoken distance measurements and 'Stop, point reached'"
    )
}

data class StakeoutVoiceSettings(
    val isEnabled: Boolean = true,
    val mode: StakeoutVoiceMode = StakeoutVoiceMode.FULL_CUES,
    val speechRate: Float = 1.1f,
    val cadenceSeconds: Int = 3
)

sealed class StakeoutSpeechCue(
    val text: String,
    val isUrgent: Boolean = false // Urgent cues flush the speech queue immediately (e.g. Stop!)
) {
    class StopPointReached(text: String = "Stop! Point reached. In tolerance.") : StakeoutSpeechCue(text, isUrgent = true)
    class TargetChanged(pointName: String, distText: String) : StakeoutSpeechCue("Target $pointName, $distText", isUrgent = true)
    class LeftTolerance(distText: String) : StakeoutSpeechCue("Off point, $distText", isUrgent = true)
    class MovingCloser(val distText: String, val cueText: String = "Moving closer, $distText") : StakeoutSpeechCue(cueText, isUrgent = false)
    class Approaching(val distText: String) : StakeoutSpeechCue("Approaching point, $distText", isUrgent = false)
    class MovingAway(val distText: String) : StakeoutSpeechCue("Moving further away, $distText", isUrgent = false)
    class TurnAlert(val directionText: String) : StakeoutSpeechCue(directionText, isUrgent = false)
    class PeriodicDistance(val distText: String) : StakeoutSpeechCue(distText, isUrgent = false)
}

/**
 * Pure domain logic evaluator for stakeout speech cues.
 * Independent of Android TTS APIs for testability and stability.
 */
class StakeoutCueEvaluator {

    var lastSpokenPointId: String? = null
    var lastSpokenDistanceMeters: Double = Double.MAX_VALUE
    var lastSpeechTimestamp: Long = 0L
    var wasInTolerance: Boolean = false

    fun reset() {
        lastSpokenPointId = null
        lastSpokenDistanceMeters = Double.MAX_VALUE
        lastSpeechTimestamp = 0L
        wasInTolerance = false
    }

    /**
     * Evaluates current guidance telemetry and determines if a voice cue should be spoken.
     *
     * @param guidance Current dynamic stakeout guidance
     * @param settings Surveyor audio preferences
     * @param currentTime Current timestamp in milliseconds
     * @return StakeoutSpeechCue to speak, or null if no announcement is needed at this time
     */
    fun evaluate(
        guidance: StakeoutGuidance?,
        settings: StakeoutVoiceSettings,
        currentTime: Long = System.currentTimeMillis()
    ): StakeoutSpeechCue? {
        if (!settings.isEnabled || guidance == null) {
            return null
        }

        val point = guidance.targetPoint.point
        val currentDist = guidance.currentDistanceMeters
        val inTolerance = guidance.isInTolerance

        // 1. Target Point Changed
        if (point.id != lastSpokenPointId) {
            lastSpokenPointId = point.id
            lastSpokenDistanceMeters = currentDist
            lastSpeechTimestamp = currentTime
            wasInTolerance = inTolerance
            val distText = formatDistanceForSpeech(currentDist)
            return StakeoutSpeechCue.TargetChanged(point.name, distText)
        }

        // 2. Tolerance Boundary Reached: "Stop, point reached!"
        if (inTolerance && !wasInTolerance) {
            wasInTolerance = true
            lastSpokenDistanceMeters = currentDist
            lastSpeechTimestamp = currentTime
            return StakeoutSpeechCue.StopPointReached()
        }

        // If currently settled in tolerance, do not repeat cues continuously
        if (inTolerance) {
            return null
        }

        // 3. User moved out of tolerance after having reached it
        if (!inTolerance && wasInTolerance) {
            wasInTolerance = false
            lastSpokenDistanceMeters = currentDist
            lastSpeechTimestamp = currentTime
            return StakeoutSpeechCue.LeftTolerance(formatDistanceForSpeech(currentDist))
        }

        // Cooldown check for regular motion cues
        val cadenceMillis = settings.cadenceSeconds * 1000L
        val timeSinceLastSpeech = currentTime - lastSpeechTimestamp
        val distanceDelta = lastSpokenDistanceMeters - currentDist // Positive if getting closer

        // 4. Close range fine stakeout thresholds (under 1 meter)
        if (currentDist <= 1.0) {
            // Fine adjustments need tighter distance delta (e.g. 15cm change or 2.5s cadence)
            if (timeSinceLastSpeech >= 2500L || abs(distanceDelta) >= 0.20) {
                if (distanceDelta > 0.08) {
                    // Moving closer in sub-meter zone
                    lastSpokenDistanceMeters = currentDist
                    lastSpeechTimestamp = currentTime
                    val cm = (currentDist * 100).roundToInt()
                    return when (settings.mode) {
                        StakeoutVoiceMode.FULL_CUES,
                        StakeoutVoiceMode.PROXIMITY_AND_STOP -> StakeoutSpeechCue.MovingCloser(
                            distText = "$cm centimeters",
                            cueText = if (cm <= 25) "$cm centimeters" else "Moving closer, $cm centimeters"
                        )
                        StakeoutVoiceMode.DISTANCE_ONLY -> StakeoutSpeechCue.PeriodicDistance("$cm centimeters")
                    }
                } else if (distanceDelta < -0.15) {
                    // Moving away in fine zone
                    lastSpokenDistanceMeters = currentDist
                    lastSpeechTimestamp = currentTime
                    val cm = (currentDist * 100).roundToInt()
                    return StakeoutSpeechCue.MovingAway("$cm centimeters")
                }
            }
            return null
        }

        // 5. Medium & Far range stakeout (> 1 meter)
        if (timeSinceLastSpeech < cadenceMillis && abs(distanceDelta) < 1.0) {
            return null
        }

        // Approaching Zone (between 1.0m and 3.5m)
        if (currentDist <= 3.5 && distanceDelta > 0.4) {
            lastSpokenDistanceMeters = currentDist
            lastSpeechTimestamp = currentTime
            val distText = formatDistanceForSpeech(currentDist)
            return when (settings.mode) {
                StakeoutVoiceMode.FULL_CUES -> StakeoutSpeechCue.Approaching(distText)
                StakeoutVoiceMode.PROXIMITY_AND_STOP -> StakeoutSpeechCue.MovingCloser(distText, "Moving closer, $distText")
                StakeoutVoiceMode.DISTANCE_ONLY -> StakeoutSpeechCue.PeriodicDistance(distText)
            }
        }

        // Significant approach: "Moving closer"
        if (distanceDelta >= 0.8) {
            lastSpokenDistanceMeters = currentDist
            lastSpeechTimestamp = currentTime
            val distText = formatDistanceForSpeech(currentDist)
            return when (settings.mode) {
                StakeoutVoiceMode.FULL_CUES,
                StakeoutVoiceMode.PROXIMITY_AND_STOP -> StakeoutSpeechCue.MovingCloser(distText)
                StakeoutVoiceMode.DISTANCE_ONLY -> StakeoutSpeechCue.PeriodicDistance(distText)
            }
        }

        // Moving further away
        if (distanceDelta <= -1.2) {
            lastSpokenDistanceMeters = currentDist
            lastSpeechTimestamp = currentTime
            val distText = formatDistanceForSpeech(currentDist)
            return StakeoutSpeechCue.MovingAway(distText)
        }

        // Directional Turn Alert (only in FULL_CUES mode, when user is walking wrong way)
        if (settings.mode == StakeoutVoiceMode.FULL_CUES && timeSinceLastSpeech >= cadenceMillis && currentDist > 2.0) {
            val relBearing = guidance.relativeBearingDegrees
            if (abs(relBearing) >= 60.0) {
                lastSpeechTimestamp = currentTime
                val turnCue = when {
                    abs(relBearing) >= 140.0 -> "Turn around"
                    relBearing > 0 -> "Turn right"
                    else -> "Turn left"
                }
                return StakeoutSpeechCue.TurnAlert(turnCue)
            }
        }

        // Periodic distance update if sufficient time has elapsed
        if (timeSinceLastSpeech >= cadenceMillis * 2) {
            lastSpokenDistanceMeters = currentDist
            lastSpeechTimestamp = currentTime
            val distText = formatDistanceForSpeech(currentDist)
            return StakeoutSpeechCue.PeriodicDistance(distText)
        }

        return null
    }

    companion object {
        fun formatDistanceForSpeech(distMeters: Double): String {
            return when {
                distMeters < 0.01 -> "0 centimeters"
                distMeters < 1.0 -> {
                    val cm = (distMeters * 100).roundToInt()
                    "$cm centimeters"
                }
                distMeters < 10.0 -> {
                    val rounded = (distMeters * 10).roundToInt() / 10.0
                    "$rounded meters"
                }
                else -> {
                    val whole = distMeters.roundToInt()
                    "$whole meters"
                }
            }
        }
    }
}

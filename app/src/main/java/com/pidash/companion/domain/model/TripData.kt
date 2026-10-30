package com.pidash.companion.domain.model

/** Snapshot of the vehicle's motion and current trip. */
data class TripData(
    val speedKmh: Int = 0,
    val speedLimitKmh: Int = 80,
    val tripDistanceKm: Double = 0.0,
    val averageSpeedKmh: Int = 0,
    val elapsedSeconds: Long = 0,
) {
    val isSpeeding: Boolean get() = speedKmh > speedLimitKmh
}

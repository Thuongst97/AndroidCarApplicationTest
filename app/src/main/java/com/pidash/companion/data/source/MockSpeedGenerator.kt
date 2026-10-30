package com.pidash.companion.data.source

import com.pidash.companion.domain.model.TripData
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlin.math.roundToInt
import kotlin.random.Random

/**
 * Simulates a realistic drive: the car picks a target speed (stop / city / highway),
 * then accelerates or brakes smoothly toward it. Trip distance and average speed are
 * integrated from the simulated speed.
 */
class MockSpeedGenerator(
    private val tickMillis: Long = 500L,
    private val random: Random = Random.Default,
) {
    fun stream(): Flow<TripData> = flow {
        var speed = 0.0
        var target = 0.0
        var ticksUntilNewTarget = 0
        var distanceKm = 0.0
        var elapsedTicks = 0L
        var speedLimit = 50

        while (true) {
            if (ticksUntilNewTarget <= 0) {
                val scenario = random.nextInt(100)
                when {
                    scenario < 15 -> { target = 0.0; speedLimit = 50 }
                    scenario < 55 -> { target = random.nextDouble(30.0, 60.0); speedLimit = 50 }
                    scenario < 80 -> { target = random.nextDouble(60.0, 95.0); speedLimit = 80 }
                    else -> { target = random.nextDouble(95.0, 135.0); speedLimit = 120 }
                }
                ticksUntilNewTarget = random.nextInt(12, 30)
            }
            ticksUntilNewTarget--

            val delta = target - speed
            val maxStep = if (delta > 0) 3.5 else 6.0 // braking is quicker than accelerating
            speed += delta.coerceIn(-maxStep, maxStep) + random.nextDouble(-0.6, 0.6)
            speed = speed.coerceAtLeast(0.0)

            elapsedTicks++
            distanceKm += speed * tickMillis / 3_600_000.0
            val hours = elapsedTicks * tickMillis / 3_600_000.0
            emit(
                TripData(
                    speedKmh = speed.roundToInt(),
                    speedLimitKmh = speedLimit,
                    tripDistanceKm = distanceKm,
                    averageSpeedKmh = if (hours > 0) (distanceKm / hours).roundToInt() else 0,
                    elapsedSeconds = elapsedTicks * tickMillis / 1000,
                ),
            )
            delay(tickMillis)
        }
    }
}

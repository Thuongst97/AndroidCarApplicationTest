package com.pidash.companion

import app.cash.turbine.test
import com.pidash.companion.data.repository.MockMediaRepository
import com.pidash.companion.data.source.MockMediaCatalog
import com.pidash.companion.data.source.MockSpeedGenerator
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class MockSpeedGeneratorTest {
    @Test
    fun `speed stays realistic and trip distance accumulates`() = runTest {
        MockSpeedGenerator(tickMillis = 500, random = Random(42)).stream().test {
            var previous = awaitItem()
            repeat(200) {
                val next = awaitItem()
                assertTrue("speed out of range: ${next.speedKmh}", next.speedKmh in 0..140)
                assertTrue("unrealistic jump ${previous.speedKmh} -> ${next.speedKmh}", kotlin.math.abs(next.speedKmh - previous.speedKmh) <= 8)
                assertTrue(next.tripDistanceKm >= previous.tripDistanceKm)
                previous = next
            }
            assertEquals(100L, previous.elapsedSeconds)
            cancelAndIgnoreRemainingEvents()
        }
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class MockMediaRepositoryTest {
    @Test
    fun `playing advances position and pause stops it`() = runTest {
        val repo = MockMediaRepository(backgroundScope)
        repo.play()
        advanceTimeBy(5_001)
        assertEquals(5_000L, repo.mediaState.value.positionMs)

        repo.pause()
        advanceTimeBy(5_000)
        assertEquals(5_000L, repo.mediaState.value.positionMs)
        assertFalse(repo.mediaState.value.isPlaying)
    }

    @Test
    fun `track auto advances at the end and skip wraps around`() = runTest {
        val repo = MockMediaRepository(backgroundScope)
        val first = MockMediaCatalog.tracks.first()
        repo.seekTo(0.999f)
        repo.play()
        advanceTimeBy(1_001)
        runCurrent()
        assertEquals(MockMediaCatalog.tracks[1].id, repo.mediaState.value.track?.id)

        repo.pause()
        repo.skipPrevious() // position 0 -> go back to first
        assertEquals(first.id, repo.mediaState.value.track?.id)
        repo.skipPrevious() // wraps to last
        assertEquals(MockMediaCatalog.tracks.last().id, repo.mediaState.value.track?.id)
    }

    @Test
    fun `skip previous restarts track when past three seconds`() = runTest {
        val repo = MockMediaRepository(backgroundScope)
        repo.seekTo(0.5f)
        repo.skipPrevious()
        assertEquals(MockMediaCatalog.tracks.first().id, repo.mediaState.value.track?.id)
        assertEquals(0L, repo.mediaState.value.positionMs)
    }
}

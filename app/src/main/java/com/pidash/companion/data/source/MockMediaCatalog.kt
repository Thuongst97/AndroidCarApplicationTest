package com.pidash.companion.data.source

import com.pidash.companion.domain.model.Track

object MockMediaCatalog {
    val tracks = listOf(
        Track("t1", "Midnight Highway", "Neon Drive", "Afterglow", 214_000, listOf(0xFF1B2A41, 0xFF3D5470, 0xFFC9A96E)),
        Track("t2", "Electric Dreams", "Polaris Nine", "Voltage", 187_000, listOf(0xFF2A1426, 0xFF6E3456, 0xFFE0B9A0)),
        Track("t3", "Amber Skyline", "The Coastline", "Golden Hour", 241_000, listOf(0xFF3B2416, 0xFFB07D48, 0xFFF0D3A0)),
        Track("t4", "Quantum Lane", "Taycan Theory", "Torque", 199_000, listOf(0xFF102521, 0xFF3E6B5E, 0xFFCFE3D6)),
        Track("t5", "Obsidian Rain", "Hyperscreen", "Dark Matter", 226_000, listOf(0xFF0D1117, 0xFF2B3240, 0xFF9AA3B5)),
    )
}

package com.pidash.companion.domain.model

enum class SpotCategory(val label: String) {
    COFFEE("Coffee"),
    GAS_STATION("Gas Station"),
    REST_STOP("Rest Stop"),
    FOOD("Food"),
}

data class Spot(
    val id: String,
    val name: String,
    val category: SpotCategory,
    val address: String,
    val distanceKm: Double,
    val latitude: Double,
    val longitude: Double,
    val isFavorite: Boolean = false,
)

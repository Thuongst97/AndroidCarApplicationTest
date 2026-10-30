package com.pidash.companion.domain.model

enum class WeatherCondition(val label: String) {
    CLEAR("Clear"),
    PARTLY_CLOUDY("Partly Cloudy"),
    CLOUDY("Cloudy"),
    RAIN("Rain"),
    STORM("Storm"),
    NIGHT_CLEAR("Clear Night"),
}

data class Weather(
    val temperatureCelsius: Int,
    val condition: WeatherCondition,
    val locationName: String,
    val humidityPercent: Int,
)

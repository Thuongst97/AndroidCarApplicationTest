package com.pidash.companion.ui.components

import com.pidash.companion.domain.model.SpotCategory

/** Stable tags for deterministic node selection in Compose UI tests. */
object TestTags {
    // Navigation
    const val NAV_RAIL = "nav_rail"
    const val NAV_DASHBOARD = "nav_dashboard"
    const val NAV_MEDIA = "nav_media"
    const val NAV_SPOTS = "nav_spots"
    const val NAV_CLOCK = "nav_clock"

    // Luxury clock
    const val LUXURY_CLOCK = "luxury_clock"
    const val LUXURY_CLOCK_TIME = "luxury_clock_time"
    const val LUXURY_CLOCK_SECONDS = "luxury_clock_seconds"
    const val LUXURY_CLOCK_DIAL = "luxury_clock_dial"
    const val LUXURY_CLOCK_DATE = "luxury_clock_date"
    const val LUXURY_CLOCK_CLOSE = "luxury_clock_close"
    const val LUXURY_CLOCK_PAGER = "luxury_clock_pager"
    const val CLOCK_PAGE_INDICATOR = "clock_page_indicator"
    const val CLOCK_PAGE_COCKPIT = "clock_page_cockpit"
    const val CLOCK_PAGE_RECTANGLE = "clock_page_rectangle"
    const val CLOCK_RECTANGLE_FACE = "clock_rectangle_face"
    const val CLOCK_PAGE_WEATHER = "clock_page_weather"
    const val CLOCK_WEATHER_TIME = "clock_weather_time"
    const val CLOCK_WEATHER_LOCATION = "clock_weather_location"
    const val CLOCK_WEATHER_TEMPERATURE = "clock_weather_temperature"
    const val CLOCK_WEATHER_CONDITION = "clock_weather_condition"

    // Dashboard
    const val DASHBOARD_SCREEN = "dashboard_screen"
    const val SPEED_GAUGE = "speed_gauge"
    const val SPEED_VALUE = "speed_value"
    const val SPEED_LIMIT = "speed_limit"
    const val TRIP_DISTANCE = "trip_distance"
    const val TRIP_AVERAGE = "trip_average"
    const val WEATHER_CARD = "weather_card"
    const val WEATHER_TEMPERATURE = "weather_temperature"
    const val WEATHER_LOCATION = "weather_location"
    const val WEATHER_ICON = "weather_icon"
    const val ACTION_HOME = "action_home"
    const val ACTION_EMERGENCY = "action_emergency"
    const val ACTION_MUTE = "action_mute"
    const val ACTION_THEME = "action_theme"
    const val MINI_PLAYER = "mini_player"
    fun quickStop(category: SpotCategory) = "quick_stop_${category.name.lowercase()}"

    // Media
    const val MEDIA_SCREEN = "media_screen"
    const val MEDIA_ARTWORK = "media_artwork"
    const val MEDIA_TITLE = "media_title"
    const val MEDIA_ARTIST = "media_artist"
    const val MEDIA_PLAY_PAUSE = "media_play_pause"
    const val MEDIA_NEXT = "media_next"
    const val MEDIA_PREVIOUS = "media_previous"
    const val MEDIA_SLIDER = "media_slider"
    const val MEDIA_POSITION = "media_position"
    const val MEDIA_DURATION = "media_duration"
    const val MEDIA_VISUALIZER = "media_visualizer"

    // Spots
    const val SPOTS_SCREEN = "spots_screen"
    const val SPOTS_LIST = "spots_list"
    const val SPOTS_EMPTY = "spots_empty"
    const val CHIP_ALL = "chip_all"
    fun chip(category: SpotCategory) = "chip_${category.name.lowercase()}"
    fun spotCard(id: String) = "spot_card_$id"
    fun spotNavigate(id: String) = "spot_navigate_$id"
}

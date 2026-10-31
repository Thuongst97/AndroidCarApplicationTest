package com.pidash.companion

/**
 * Test tags for runners that filter by annotation (AndroidJUnitRunner `-e annotation`, ATR
 * `--include Smoke`). Put a tag on a test method or on a whole test class.
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
annotation class Smoke

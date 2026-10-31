import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("jvm") version "2.4.21"
    kotlin("plugin.serialization") version "2.4.21"
    application
}

group = "dev.atr"
version = "1.0.0"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions { jvmTarget = JvmTarget.JVM_17 }
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.12.0")
    implementation("org.yaml:snakeyaml:2.7")
    testImplementation(kotlin("test"))
}

application {
    mainClass = "atr.MainKt"
    applicationName = "atr"
}

tasks.test {
    useJUnitPlatform()
}

// Bake the version into the jar so `atr version` and reports can show it.
tasks.processResources {
    val v = project.version.toString()
    inputs.property("version", v)
    filesMatching("atr-version.properties") { expand("version" to v) }
}

// Distribution: atr-<version>.zip = bin/, lib/, README.md, install scripts, example config.
tasks.named<Zip>("distZip") {
    filesMatching("**/install.sh") { permissions { unix("rwxr-xr-x") } }
}

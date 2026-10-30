plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    jacoco
}

jacoco {
    toolVersion = libs.versions.jacoco.get()
}

android {
    namespace = "com.pidash.companion"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.pidash.companion"
        minSdk = 28
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        // Files written through TestStorage (failure screenshots) are pulled into
        // build/outputs/connected_android_test_additional_output after connected tests.
        testInstrumentationRunnerArguments["useTestStorageService"] = "true"
    }

    buildTypes {
        debug {
            // JaCoCo: instrument app classes for both JVM unit tests and on-device UI tests.
            enableUnitTestCoverage = true
            enableAndroidTestCoverage = true
        }
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
    }
    packaging {
        resources {
            excludes += setOf("/META-INF/{AL2.0,LGPL2.1}", "META-INF/LICENSE*.md")
        }
    }
    testOptions {
        unitTests.isReturnDefaultValues = true
    }
    testCoverage {
        jacocoVersion = libs.versions.jacoco.get()
    }
}

// ---------------------------------------------------------------------------------------------
// Merged coverage: JVM unit tests (.exec) + on-device Compose UI tests (.ec) in one report.
//   ./gradlew :app:jacocoMergedReport
// Reuse existing coverage data without re-running tests:
//   ./gradlew :app:jacocoMergedReport -x testDebugUnitTest -x connectedDebugAndroidTest
// ---------------------------------------------------------------------------------------------
val coverageExclusions = listOf(
    // Android / build generated
    "**/R.class", "**/R$*.class", "**/BuildConfig.*", "**/Manifest*.*",
    // Hilt / Dagger generated
    "hilt_aggregated_deps/**", "dagger/**", "**/Hilt_*.*", "**/*_HiltModules*.*", "**/*_Factory*.*",
    "**/*_MembersInjector.*", "**/*_GeneratedInjector.*", "**/*_ComponentTreeDeps.*", "**/*_HiltComponents*.*",
    "**/Dagger*.*", "**/*Module_*.*", "**/*_Impl*.*",
    // Compose compiler generated lambda holders
    "**/ComposableSingletons*.*",
)

tasks.register<JacocoReport>("jacocoMergedReport") {
    group = "verification"
    description = "Merged JaCoCo coverage of debug unit tests and connected Compose UI tests."
    dependsOn("testDebugUnitTest", "connectedDebugAndroidTest")

    val build = layout.buildDirectory
    executionData.setFrom(
        fileTree(build) {
            include("outputs/unit_test_code_coverage/debugUnitTest/*.exec")
            include("outputs/code_coverage/debugAndroidTest/connected/**/*.ec")
        },
    )
    // Must be the classes JaCoCo instrumented, i.e. after Hilt's bytecode transform,
    // otherwise transformed classes (MainActivity, PiDashApplication) report as mismatched.
    classDirectories.setFrom(
        fileTree(build.dir("intermediates/classes/debug/transformDebugClassesWithAsm/dirs")) { exclude(coverageExclusions) },
    )
    sourceDirectories.setFrom(files("src/main/java"))

    reports {
        html.required = true
        xml.required = true
        csv.required = false
        html.outputLocation = build.dir("reports/coverage/merged/html")
        xml.outputLocation = build.file("reports/coverage/merged/coverage.xml")
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.hilt.android)
    implementation(libs.androidx.hilt.navigation.compose)
    ksp(libs.hilt.compiler)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.mockk)
    testImplementation(libs.turbine)

    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
    // Espresso < 3.7 calls InputManager.getInstance(), which no longer exists on Android 16.
    androidTestImplementation(libs.androidx.test.espresso.core)
    androidTestImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.mockk.android)
    androidTestImplementation(libs.turbine)
    androidTestImplementation(libs.androidx.test.storage)
    androidTestUtil(libs.androidx.test.services)

    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}

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

// ---------------------------------------------------------------------------------------------
// Marathon: attach the tests' own screenshots and a video clip per test to Marathon's HTML report.
//   .\scripts\marathon-with-video.ps1          (records video, runs Marathon, then calls this task)
//   ./gradlew :app:attachMarathonEvidence      (-PmarathonReport=app/build/reports/marathon-review)
// Marathon's own screen capture doesn't work on the Raspberry Pi, so each test page has an empty
// "screenshot" field and no videos. This fills them in:
// - screenshot: evidence/<Class>_<method>.png (passed) or screenshots/<Class>_<method>.png (failed),
//   pulled from the device into <report>/device-files/test-screenshots/
// - videos: <report>/video/run.mp4#t=<start>,<end> — the test's own time range in the run video
//   recorded by scrcpy (run.start holds the epoch ms of the video's first frame).
// ---------------------------------------------------------------------------------------------
tasks.register("attachMarathonEvidence") {
    group = "verification"
    description = "Embeds evidence screenshots and per-test video clips into Marathon's HTML report."
    val reportDir = rootProject.layout.projectDirectory.dir(
        providers.gradleProperty("marathonReport").getOrElse("app/build/reports/marathon"),
    ).asFile

    doLast {
        val html = File(reportDir, "html")
        val pulled = File(reportDir, "device-files/test-screenshots")
        require(html.isDirectory) { "No Marathon HTML report in $reportDir — run marathon first." }
        val target = File(html, "evidence").apply { mkdirs() }

        // Per-test timing from Marathon (timestamp = test start, epoch ms). Last attempt wins on retries.
        val timings = mutableMapOf<String, Pair<Long, Long>>()
        val raw = File(reportDir, "test_result/raw.json")
        if (raw.isFile) {
            @Suppress("UNCHECKED_CAST")
            (groovy.json.JsonSlurper().parse(raw) as List<Map<String, Any?>>).forEach { t ->
                timings["${t["class"]}#${t["method"]}"] = (t["timestamp"] as Number).toLong() to (t["duration"] as Number).toLong()
            }
        }
        val video = File(reportDir, "video/run.mp4")
        val videoStart = File(reportDir, "video/run.start").takeIf { it.isFile }?.readText()?.trim()?.toLongOrNull()
            ?.takeIf { video.isFile }

        val classRegex = Regex("\"class_name\":\"([^\"]+)\"")
        val nameRegex = Regex("\"name\":\"([^\"]+)\"")
        val statusRegex = Regex("\"status\":\"([^\"]+)\"")
        var screenshots = 0
        var clips = 0

        html.resolve("pools").walkTopDown()
            .filter { it.isFile && it.extension == "html" && it.name != "index.html" && it.parentFile.name != "logs" }
            .forEach { page ->
                var text = page.readText()
                if (!text.contains("window.test")) return@forEach
                val cls = classRegex.find(text)?.groupValues?.get(1) ?: return@forEach
                val method = nameRegex.find(text)?.groupValues?.get(1) ?: return@forEach
                val failed = statusRegex.find(text)?.groupValues?.get(1) == "failed"
                fun rel(file: File) = page.parentFile.toPath().relativize(file.toPath()).toString().replace('\\', '/')

                // Screenshot
                val fileName = "${cls}_$method.png"
                listOf(if (failed) "screenshots" else "evidence", "evidence", "screenshots")
                    .map { File(pulled, "$it/$fileName") }.firstOrNull { it.isFile }
                    ?.let { source ->
                        val copy = File(target, fileName)
                        source.copyTo(copy, overwrite = true)
                        text = text.replace(Regex("\"screenshot\":\"[^\"]*\""), "\"screenshot\":\"${rel(copy)}\"")
                        screenshots++
                    }

                // Video clip: whole seconds only (the report's player rejects decimals in #t=).
                if (videoStart != null) {
                    timings["$cls#$method"]?.let { (start, duration) ->
                        val from = ((start - videoStart) / 1000 - 1).coerceAtLeast(0)
                        val to = (start + duration - videoStart) / 1000 + 2
                        text = text.replace(Regex("\"videos\":\\[[^\\]]*\\]"), "\"videos\":[\"${rel(video)}#t=$from,$to\"]")
                        clips++
                    }
                }
                page.writeText(text)
            }
        logger.lifecycle(
            "Marathon report: $screenshots screenshot(s), $clips video clip(s)" +
                (if (videoStart == null) " (no video/run.mp4 found)" else "") + " → ${html.resolve("index.html")}",
        )
    }
}

// ---------------------------------------------------------------------------------------------
// Failure screenshots in Gradle's connected-test HTML report.
// ScreenshotOnFailureRule saves screenshots/<Class>_<method>.png through TestStorage and Gradle pulls
// them into build/outputs/connected_android_test_additional_output, but Gradle's HTML report does not
// show them. This task copies them into the report and shows each one under its failed test.
// Runs automatically after connectedDebugAndroidTest, also when tests fail.
// ---------------------------------------------------------------------------------------------
val attachFailureScreenshots = tasks.register("attachFailureScreenshots") {
    group = "verification"
    description = "Shows the failure screenshot under each failed test in the connected-test HTML report."
    val reportDir = layout.buildDirectory.dir("reports/androidTests/connected/debug")
    val pulledDir = layout.buildDirectory.dir("outputs/connected_android_test_additional_output/debugAndroidTest/connected")

    doLast {
        val report = reportDir.get().asFile
        if (!File(report, "index.html").isFile) {
            logger.lifecycle("No connected-test HTML report in $report; nothing to attach.")
            return@doLast
        }
        val shots = pulledDir.get().asFile.walkTopDown()
            .filter { it.isFile && it.extension == "png" && it.parentFile.name == "screenshots" }
            .associateBy { it.name }
        val target = File(report, "screenshots").apply { deleteRecursively(); mkdirs() }
        // One block per failed test: <h3 class="failures">method</h3> <span class="code"><pre>…</pre></span>
        val failedTest = Regex("""<h3 class="failures">([^<]+)</h3>(.*?)</span>""", RegexOption.DOT_MATCHES_ALL)
        var attached = 0
        var missing = 0

        report.listFiles { f -> f.extension == "html" }.orEmpty().forEach { page ->
            var text = page.readText()
            if (!text.contains("<h3 class=\"failures\">") || text.contains("failure-screenshot")) return@forEach
            val simpleClass = page.name.removeSuffix(".html").substringAfterLast('.')
            text = failedTest.replace(text) { match ->
                val name = "${simpleClass}_${match.groupValues[1].trim()}.png"
                val shot = shots[name]
                val evidence = if (shot != null) {
                    shot.copyTo(File(target, name), overwrite = true)
                    attached++
                    """<div class="failure-screenshot"><p><b>Screen at the moment of failure</b></p>""" +
                        """<a href="screenshots/$name"><img src="screenshots/$name" alt="$name" """ +
                        """style="max-width:480px;border:1px solid #ccc;border-radius:4px"/></a></div>"""
                } else {
                    missing++
                    """<p class="failure-screenshot"><i>No screenshot: the UI no longer existed when the test """ +
                        """failed (for example, the app closed). See the logcat in the results folder.</i></p>"""
                }
                match.value + evidence
            }
            page.writeText(text)
        }
        logger.lifecycle("Failure screenshots: $attached attached, $missing unavailable → ${File(report, "index.html")}")
    }
}
tasks.configureEach {
    if (name == "connectedDebugAndroidTest") finalizedBy(attachFailureScreenshots)
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

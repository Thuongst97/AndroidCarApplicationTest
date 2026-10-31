package atr

import java.io.File
import java.net.InetAddress

/** Orchestrates a run: device → install → discover → select → execute each test → reports. */
class Runner(
    private val config: Config,
    private val console: Console = Console(),
) {
    data class Prepared(val adb: Adb, val instrumentation: Instrumentation, val device: DeviceInfo)

    /** Connects to the device, installs APKs (optional) and resolves the instrumentation. */
    fun prepare(): Prepared {
        val adbExe = Adb.locate() ?: throw UsageException("adb not found. Install Android platform-tools and set ANDROID_HOME or PATH.")
        val serial = config.serial ?: Adb.connectedDevices(adbExe).let { devices ->
            when (devices.size) {
                0 -> throw UsageException("No device connected (adb devices is empty).")
                1 -> devices.single()
                else -> throw UsageException("Several devices connected: ${devices.joinToString()}. Choose one with --serial or device.serial.")
            }
        }
        val adb = Adb(adbExe, serial)
        if (!Adb.connectedDevices(adbExe).contains(serial)) throw UsageException("Device '$serial' is not connected. Check `adb devices`.")
        val device = adb.deviceInfo()
        console.info("Device $serial - ${device.manufacturer} ${device.model}, Android ${device.androidRelease} (SDK ${device.sdk})")

        if (config.install) {
            listOfNotNull(config.appApk, config.testApk).forEach { apk ->
                if (!apk.isFile) throw UsageException("APK not found: ${apk.absolutePath}. Build it first (e.g. gradlew assembleDebug assembleDebugAndroidTest).")
                console.info("Installing ${apk.name}")
                val r = adb.install(apk)
                if (!r.ok || !r.stdout.contains("Success")) throw IllegalStateException("Install failed for ${apk.name}:\n${r.output}")
            }
        }
        return Prepared(adb, resolveInstrumentation(adb), device)
    }

    private fun resolveInstrumentation(adb: Adb): Instrumentation {
        val all = adb.instrumentations()
        config.instrumentation?.let { configured ->
            return all.firstOrNull { it.component == configured }
                ?: Instrumentation(configured.substringBefore('/'), configured.substringAfter('/'), config.appPackage ?: "")
        }
        val candidates = config.appPackage?.let { pkg -> all.filter { it.targetPackage == pkg } } ?: all
        return when (candidates.size) {
            1 -> candidates.single()
            0 -> throw UsageException("No test instrumentation found on the device" +
                (config.appPackage?.let { " for app '$it'" } ?: "") + ". Install the test APK or set 'instrumentation:'.")
            else -> throw UsageException("Several instrumentations found; set 'appPackage:' or 'instrumentation:' in the config:\n" +
                candidates.joinToString("\n") { "  ${it.component} (target=${it.targetPackage})" })
        }
    }

    /** Lists tests via the runner itself; tags become annotation / notAnnotation filters. */
    fun discover(p: Prepared, includes: List<String>, excludes: List<String>): List<TestId> {
        val notAnnotation = excludes.map(config::annotationFor)
        val filterSets: List<List<String>> = if (includes.isEmpty()) listOf(emptyList())
        else includes.map { listOf("-e", "annotation", config.annotationFor(it)) }

        val found = LinkedHashSet<TestId>()
        for (filter in filterSets) {
            val args = mutableListOf("shell", "am", "instrument", "-r", "-w", "-e", "log", "true")
            args += filter
            if (notAnnotation.isNotEmpty()) args += listOf("-e", "notAnnotation", notAnnotation.joinToString(","))
            args += p.instrumentation.component
            val r = p.adb.exec(args, timeoutMs = 300_000)
            val output = InstrumentationParser.parse(r.stdout)
            output.failure?.let { throw IllegalStateException("Test discovery failed: $it") }
            found += InstrumentationParser.listedTests(output)
        }
        return found.toList()
    }

    /** Runs the selected tests one by one and returns the full result. */
    fun execute(p: Prepared, tests: List<TestId>, selection: List<String>, command: String): RunResult {
        val started = System.currentTimeMillis()
        val artifactsRoot = File(config.outputDir, "artifacts")
        val recorder = recorderOrNull(p.adb)
        console.header(config.name, "${p.device.model} | Android ${p.device.androidRelease} | ${p.device.serial} | ${tests.size} tests")

        val tagsByTest = tagsOf(p, tests)
        val results = mutableListOf<TestResult>()
        var currentClass: String? = null
        for ((index, test) in tests.withIndex()) {
            if (test.className != currentClass) {
                if (currentClass != null) console.line()
                console.suite(test.className)
                currentClass = test.className
            }
            val attempts = mutableListOf<Attempt>()
            for (n in 1..(config.retries + 1)) {
                val attempt = runAttempt(p, test, n, File(artifactsRoot, "${testDirName(index + 1, test)}/a$n"), recorder)
                attempts += attempt
                if (attempt.status == Status.PASS || attempt.status == Status.SKIP) break
                if (n <= config.retries) console.line("    retrying (${attempt.status}: ${attempt.message?.take(90) ?: "no message"})")
            }
            val result = TestResult(test, attempts.last().status, attempts, tags = tagsByTest[test].orEmpty())
            results += result
            console.test(test.method, result.status, result.durationMs,
                note = when {
                    result.flaky -> "flaky: passed on attempt ${attempts.size}"
                    result.status == Status.FAIL || result.status == Status.ERROR -> result.finalAttempt.message?.take(140)
                    else -> null
                })
        }

        val info = RunInfo(
            name = config.name,
            toolVersion = Version.current,
            startedAt = started,
            finishedAt = System.currentTimeMillis(),
            device = p.device,
            instrumentation = p.instrumentation.component,
            appPackage = p.instrumentation.targetPackage.ifBlank { null },
            instrumentationArgs = config.instrumentationArgs,
            selection = selection,
            host = runCatching { InetAddress.getLocalHost().hostName }.getOrDefault(""),
            command = command,
        )
        return RunResult(info, results)
    }

    /** Tag aliases per test: one discovery per configured tag (annotation filter). */
    private fun tagsOf(p: Prepared, tests: List<TestId>): Map<TestId, List<String>> {
        if (config.tags.isEmpty()) return emptyMap()
        val selected = tests.toSet()
        val tags = mutableMapOf<TestId, MutableList<String>>()
        config.tags.keys.forEach { alias ->
            discover(p, listOf(alias), emptyList()).filter { it in selected }.forEach { tags.getOrPut(it) { mutableListOf() } += alias }
        }
        return tags
    }

    private fun recorderOrNull(adb: Adb): ScrcpyRecorder? {
        if (config.video == VideoMode.NEVER) return null
        val exe = ScrcpyRecorder.locate(config.scrcpyPath)
        if (exe == null) {
            console.warn("scrcpy not found ('${config.scrcpyPath}'): videos disabled. Install scrcpy or set scrcpy.path.")
            return null
        }
        return ScrcpyRecorder(exe, adb, config.scrcpyArgs)
    }

    private fun runAttempt(p: Prepared, test: TestId, n: Int, dir: File, recorder: ScrcpyRecorder?): Attempt {
        dir.mkdirs()
        val adb = p.adb
        // Clean device state for this attempt
        config.pullPaths.forEach { adb.shell("rm -rf '$it'/*", 15_000) }
        if (config.logcat) adb.clearLogcat()

        val session = recorder?.let { r ->
            runCatching { r.start(File(dir, "video.mp4")) }
                .onFailure { console.warn("Video not recorded: ${it.message?.lineSequence()?.firstOrNull()}") }
                .getOrNull()
        }

        val args = mutableListOf("shell", "am", "instrument", "-r", "-w", "-e", "class", test.fullName)
        config.instrumentationArgs.forEach { (k, v) -> args += listOf("-e", k, v) }
        args += p.instrumentation.component
        val startedAt = System.currentTimeMillis()
        val exec = adb.exec(args, timeoutMs = config.timeoutSeconds * 1000L)
        val durationMs = System.currentTimeMillis() - startedAt
        if (exec.timedOut) p.instrumentation.targetPackage.takeIf { it.isNotBlank() }?.let(adb::forceStop)

        val outcome = InstrumentationParser.outcomeOf(test, InstrumentationParser.parse(exec.stdout), exec.timedOut)
        val failed = outcome.status == Status.FAIL || outcome.status == Status.ERROR

        // Collect evidence
        val videoOk = session?.stop() == true
        val video = File(dir, "video.mp4")
        if (videoOk && config.video == VideoMode.ON_FAILURE && !failed) video.delete()

        File(dir, "instrumentation.txt").writeText(exec.output)
        if (config.logcat) File(dir, "logcat.txt").writeText(adb.dumpLogcat())

        val pulled = mutableListOf<File>()
        config.pullPaths.forEach { remote ->
            val target = File(dir, "files")
            val r = adb.pull(remote, target)
            if (!r.ok) console.warn("Could not pull $remote (exit ${r.exitCode}): ${r.output.trim().lines().joinToString(" / ")}")
            pulled += target.walkTopDown().filter { it.isFile }
        }
        val images = pulled.filter { it.extension.lowercase() in setOf("png", "jpg", "jpeg", "webp", "gif") }

        fun rel(f: File) = f.relativeTo(config.outputDir).invariantSeparatorsPath
        return Attempt(
            index = n,
            status = outcome.status,
            startedAt = startedAt,
            durationMs = durationMs,
            message = outcome.message,
            stackTrace = outcome.stackTrace,
            artifacts = Artifacts(
                video = video.takeIf { it.isFile && it.length() > 0 }?.let(::rel),
                logcat = File(dir, "logcat.txt").takeIf { it.isFile }?.let(::rel),
                instrumentation = rel(File(dir, "instrumentation.txt")),
                screenshots = images.map(::rel).sorted(),
                files = (pulled - images.toSet()).map(::rel).sorted(),
            ),
        )
    }

    /**
     * Short, readable folder name, e.g. `t001-NavigationTest-railReturnsToDash`. Kept short on purpose:
     * Windows tools (adb pull) fail on paths longer than 260 characters, and pulled device files add
     * their own sub-folders and long file names below this folder.
     */
    private fun testDirName(index: Int, test: TestId): String {
        val readable = "${test.simpleClassName}-${test.method}".replace(Regex("[^A-Za-z0-9._-]"), "_").take(32)
        return "t%03d-%s".format(index, readable)
    }
}

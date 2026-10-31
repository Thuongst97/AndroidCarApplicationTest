package atr

import kotlinx.serialization.Serializable

/** Final status of a test case or a single attempt. */
@Serializable
enum class Status { PASS, FAIL, ERROR, SKIP }

/** A test case as discovered on the device. */
@Serializable
data class TestId(val className: String, val method: String) {
    val fullName: String get() = "$className#$method"
    val simpleClassName: String get() = className.substringAfterLast('.')
    override fun toString() = fullName
}

/** Evidence collected for one attempt; paths are relative to the output directory. */
@Serializable
data class Artifacts(
    val video: String? = null,
    val logcat: String? = null,
    val instrumentation: String? = null,
    val screenshots: List<String> = emptyList(),
    val files: List<String> = emptyList(),
)

@Serializable
data class Attempt(
    val index: Int,
    val status: Status,
    val startedAt: Long,
    val durationMs: Long,
    val message: String? = null,
    val stackTrace: String? = null,
    val artifacts: Artifacts = Artifacts(),
)

@Serializable
data class TestResult(
    val id: TestId,
    val status: Status,
    val attempts: List<Attempt>,
    val durationMs: Long = attempts.sumOf { it.durationMs },
    /** Passed only after at least one failed attempt. */
    val flaky: Boolean = status == Status.PASS && attempts.any { it.status != Status.PASS },
    /** Tag aliases from the config whose annotation this test carries. */
    val tags: List<String> = emptyList(),
) {
    val finalAttempt: Attempt get() = attempts.last()
}

@Serializable
data class DeviceInfo(
    val serial: String,
    val manufacturer: String = "",
    val model: String = "",
    val androidRelease: String = "",
    val sdk: String = "",
)

@Serializable
data class RunInfo(
    val name: String,
    val toolVersion: String,
    val startedAt: Long,
    val finishedAt: Long,
    val device: DeviceInfo,
    val instrumentation: String,
    val appPackage: String?,
    val instrumentationArgs: Map<String, String>,
    val selection: List<String>,
    val host: String,
    val command: String,
)

@Serializable
data class RunResult(
    val info: RunInfo,
    val tests: List<TestResult>,
    val stats: Stats = Stats.of(tests),
)

@Serializable
data class Stats(
    val total: Int,
    val passed: Int,
    val failed: Int,
    val errors: Int,
    val skipped: Int,
    val flaky: Int,
    /** Passed / (total - skipped), in percent. */
    val passRate: Double = if (total - skipped == 0) 100.0 else passed * 100.0 / (total - skipped),
) {
    /** Failed + errors: the number reported as exit code. */
    val notPassed: Int get() = failed + errors

    companion object {
        fun of(tests: List<TestResult>) = Stats(
            total = tests.size,
            passed = tests.count { it.status == Status.PASS },
            failed = tests.count { it.status == Status.FAIL },
            errors = tests.count { it.status == Status.ERROR },
            skipped = tests.count { it.status == Status.SKIP },
            flaky = tests.count { it.flaky },
        )
    }
}

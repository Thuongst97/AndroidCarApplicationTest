package atr

/**
 * Parses the output of `am instrument -r` (raw mode).
 *
 * The output is a sequence of status blocks:
 * ```
 * INSTRUMENTATION_STATUS: class=com.app.FooTest
 * INSTRUMENTATION_STATUS: test=bar
 * INSTRUMENTATION_STATUS: stack=java.lang.AssertionError: …   ← values may span several lines
 *     at com.app.FooTest.bar(FooTest.kt:12)
 * INSTRUMENTATION_STATUS_CODE: -2
 * …
 * INSTRUMENTATION_RESULT: stream=…
 * INSTRUMENTATION_CODE: -1
 * ```
 */
object InstrumentationParser {

    data class StatusBlock(val code: Int, val values: Map<String, String>) {
        val className: String? get() = values["class"]
        val test: String? get() = values["test"]
    }

    data class Output(
        val blocks: List<StatusBlock>,
        val result: Map<String, String>,
        val code: Int?,
        /** Lines such as `INSTRUMENTATION_FAILED: …` reported before any test ran. */
        val failure: String?,
    )

    const val CODE_START = 1
    const val CODE_OK = 0
    const val CODE_ERROR = -1
    const val CODE_FAILURE = -2
    const val CODE_IGNORED = -3
    const val CODE_ASSUMPTION = -4

    fun parse(text: String): Output {
        val blocks = mutableListOf<StatusBlock>()
        val result = linkedMapOf<String, String>()
        var current = linkedMapOf<String, String>()
        var active: MutableMap<String, String> = current
        var lastKey: String? = null
        var code: Int? = null
        var failure: String? = null

        for (raw in text.lineSequence()) {
            val line = raw.trimEnd('\r')
            when {
                line.startsWith("INSTRUMENTATION_STATUS: ") -> {
                    val (k, v) = keyValue(line.removePrefix("INSTRUMENTATION_STATUS: "))
                    current[k] = v
                    active = current
                    lastKey = k
                }
                line.startsWith("INSTRUMENTATION_STATUS_CODE: ") -> {
                    blocks += StatusBlock(line.substringAfter(": ").trim().toIntOrNull() ?: CODE_ERROR, current.toMap())
                    current = linkedMapOf()
                    active = current
                    lastKey = null
                }
                line.startsWith("INSTRUMENTATION_RESULT: ") -> {
                    val (k, v) = keyValue(line.removePrefix("INSTRUMENTATION_RESULT: "))
                    result[k] = v
                    active = result
                    lastKey = k
                }
                line.startsWith("INSTRUMENTATION_CODE: ") -> {
                    code = line.substringAfter(": ").trim().toIntOrNull()
                    lastKey = null
                }
                line.startsWith("INSTRUMENTATION_FAILED: ") -> {
                    failure = line.removePrefix("INSTRUMENTATION_FAILED: ").trim()
                    lastKey = null
                }
                lastKey != null -> {
                    val key = lastKey
                    active[key] = active[key] + "\n" + line
                }
            }
        }
        return Output(blocks, result, code, failure)
    }

    private fun keyValue(s: String): Pair<String, String> {
        val i = s.indexOf('=')
        return if (i < 0) s to "" else s.substring(0, i) to s.substring(i + 1)
    }

    /** Tests reported by a `-e log true` (list-only) run, in order. */
    fun listedTests(output: Output): List<TestId> =
        output.blocks.filter { it.code == CODE_START }
            .mapNotNull { b -> b.className?.let { c -> b.test?.let { t -> TestId(c, t) } } }
            .distinct()

    data class Outcome(val status: Status, val message: String?, val stackTrace: String?)

    /** Outcome of a single test from a run that targeted only that test. */
    fun outcomeOf(test: TestId, output: Output, timedOut: Boolean): Outcome {
        if (timedOut) return Outcome(Status.ERROR, "Timed out: test did not finish within the time limit", null)
        output.failure?.let { return Outcome(Status.ERROR, "Instrumentation failed: $it", null) }

        val end = output.blocks.lastOrNull { it.code != CODE_START && it.className == test.className && it.test == test.method }
        if (end == null) {
            val shortMsg = output.result["shortMsg"]
            val longMsg = output.result["longMsg"]
            val msg = listOfNotNull(shortMsg, longMsg).joinToString(": ").ifBlank {
                if (output.blocks.none { it.code == CODE_START }) "Test was not found or did not start" else "Test did not report a result (process crashed?)"
            }
            return Outcome(Status.ERROR, msg, output.result["stream"]?.trim()?.takeIf { it.isNotEmpty() })
        }
        val stack = end.values["stack"]?.trim()?.takeIf { it.isNotEmpty() }
        val message = stack?.lineSequence()?.firstOrNull()?.trim()
        val status = when (end.code) {
            CODE_OK -> Status.PASS
            CODE_FAILURE -> Status.FAIL
            CODE_IGNORED, CODE_ASSUMPTION -> Status.SKIP
            else -> Status.ERROR
        }
        return Outcome(status, if (status == Status.PASS) null else message, if (status == Status.PASS) null else stack)
    }
}

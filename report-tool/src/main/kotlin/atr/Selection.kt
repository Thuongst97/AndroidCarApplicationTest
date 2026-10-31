package atr

import kotlinx.serialization.json.Json
import java.io.File

/**
 * Robot Framework–style test selection.
 *
 * - `--test` patterns match the method name, `SimpleClass#method` or `pkg.Class#method`.
 * - `--suite` patterns match the simple or fully-qualified class name.
 * - Patterns are case-insensitive globs: `*` any characters, `?` one character.
 * - With both `--suite` and `--test`, a test must match both (as in Robot Framework).
 * - `--rerunfailed` keeps only tests that failed or errored in a previous output.json.
 * Tags (`--include` / `--exclude`) are applied during discovery via runner annotation filters.
 */
class Selection(
    private val tests: List<String> = emptyList(),
    private val suites: List<String> = emptyList(),
    private val onlyIds: Set<String>? = null,
) {
    private val testRegexes = tests.map(::glob)
    private val suiteRegexes = suites.map(::glob)

    fun matches(id: TestId): Boolean {
        if (onlyIds != null && id.fullName !in onlyIds) return false
        if (suiteRegexes.isNotEmpty() && suiteRegexes.none { it.matches(id.className) || it.matches(id.simpleClassName) }) return false
        if (testRegexes.isNotEmpty() && testRegexes.none { r ->
                r.matches(id.method) || r.matches("${id.simpleClassName}#${id.method}") || r.matches(id.fullName)
            }
        ) return false
        return true
    }

    fun apply(all: List<TestId>): List<TestId> = all.filter(::matches)

    /** Human-readable description for reports, e.g. ["--test *mute*", "--include Smoke"]. */
    fun describe(): List<String> = tests.map { "--test $it" } + suites.map { "--suite $it" } +
        (onlyIds?.let { listOf("--rerunfailed (${it.size} tests)") } ?: emptyList())

    companion object {
        fun glob(pattern: String): Regex {
            val sb = StringBuilder()
            for (c in pattern.trim()) {
                when (c) {
                    '*' -> sb.append(".*")
                    '?' -> sb.append('.')
                    else -> sb.append(Regex.escape(c.toString()))
                }
            }
            return Regex(sb.toString(), RegexOption.IGNORE_CASE)
        }

        /** IDs of tests that did not pass in a previous run. */
        fun failedIn(outputJson: File): Set<String> {
            if (!outputJson.isFile) throw UsageException("--rerunfailed: file not found: ${outputJson.absolutePath}")
            val previous = JsonReport.read(outputJson)
            val failed = previous.tests.filter { it.status == Status.FAIL || it.status == Status.ERROR }.map { it.id.fullName }.toSet()
            if (failed.isEmpty()) throw UsageException("--rerunfailed: all tests passed in ${outputJson.name}; nothing to rerun.")
            return failed
        }
    }
}

object JsonReport {
    val json = Json { prettyPrint = true; encodeDefaults = true; ignoreUnknownKeys = true }

    fun write(result: RunResult, file: File) {
        file.parentFile.mkdirs()
        file.writeText(json.encodeToString(RunResult.serializer(), result))
    }

    fun read(file: File): RunResult = json.decodeFromString(RunResult.serializer(), file.readText())
}

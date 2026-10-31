package atr

import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Writes the run as a Robot Framework result file (`output.xml`) so that `rebot` can build
 * Robot's report.html / log.html and merge it with the project's other Robot results:
 *
 *     rebot --name Project robot-tests.xml android.xml
 *
 * Mapping: run → top suite, test class → suite, test method → test, annotations → tags,
 * each attempt → keyword `ATR.Run Instrumentation Test` that logs video, screenshots, logs and
 * the failure. Artifact links are relative to the folder of the written file, so generate
 * log.html in that same folder (rebot's default when `--outputdir` is that folder).
 *
 * Schema 5 is Robot Framework 7.x; [legacy] writes schema 4 for Robot Framework 6.x.
 */
object RobotXmlReport {
    const val FLAKY_TAG = "flaky"
    private const val KEYWORD = "Run Instrumentation Test"
    private const val OWNER = "ATR"

    fun write(result: RunResult, outputDir: File, file: File, legacy: Boolean = false) {
        val target = file.absoluteFile
        target.parentFile.mkdirs()
        target.writeText(render(result, outputDir.absoluteFile, target.parentFile, legacy))
    }

    fun render(result: RunResult, outputDir: File, xmlDir: File, legacy: Boolean = false): String =
        Writer(result, outputDir, xmlDir, legacy).render()

    private class Writer(val result: RunResult, val outputDir: File, val xmlDir: File, val legacy: Boolean) {
        private val sb = StringBuilder()
        private val info = result.info

        fun render(): String {
            sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
            sb.append("<robot generator=\"ATR ${attr(info.toolVersion)}\" generated=\"${time(info.finishedAt)}\" rpa=\"false\" schemaversion=\"${if (legacy) 4 else 5}\">\n")
            sb.append("<suite id=\"s1\" name=\"${attr(info.name)}\">\n")
            result.tests.groupBy { it.id.className }.entries.forEachIndexed { i, (cls, tests) -> suite("s1-s${i + 1}", cls, tests) }
            element("doc", "Android instrumented tests run by ATR ${info.toolVersion} on ${device()}.")
            metadata()
            status(statusOf(result.tests), info.startedAt, info.finishedAt)
            sb.append("</suite>\n")
            sb.append("<statistics>\n</statistics>\n<errors>\n</errors>\n</robot>\n")
            return sb.toString()
        }

        private fun suite(id: String, cls: String, tests: List<TestResult>) {
            sb.append("<suite id=\"$id\" name=\"${attr(cls.substringAfterLast('.'))}\">\n")
            tests.forEachIndexed { i, t -> test("$id-t${i + 1}", t) }
            element("doc", cls)
            status(statusOf(tests), tests.first().attempts.first().startedAt, end(tests.last()))
            sb.append("</suite>\n")
        }

        private fun test(id: String, t: TestResult) {
            sb.append("<test id=\"$id\" name=\"${attr(t.id.method)}\">\n")
            t.attempts.forEach { attempt(t, it) }
            element("doc", t.id.fullName)
            (t.tags + if (t.flaky) listOf(FLAKY_TAG) else emptyList()).distinct().forEach { element("tag", it) }
            val message = when {
                t.flaky -> "Passed on attempt ${t.attempts.size} of ${t.attempts.size} (flaky)."
                else -> message(t.finalAttempt)
            }
            status(robotStatus(t.status), t.attempts.first().startedAt, end(t), message)
            sb.append("</test>\n")
        }

        private fun attempt(t: TestResult, a: Attempt) {
            val ownerAttr = if (legacy) "library" else "owner"
            sb.append("<kw name=\"$KEYWORD\" $ownerAttr=\"$OWNER\">\n")
            val start = a.startedAt
            msg(start, "INFO", "Attempt ${a.index} of ${t.attempts.size} on ${device()} - ${a.status} in ${"%.1f".format(java.util.Locale.ROOT, a.durationMs / 1000.0)} s")
            val art = a.artifacts
            art.video?.let { v ->
                val src = link(v)
                msg(start, "INFO", "<video src=\"${attr(src)}#t=0.1\" controls preload=\"metadata\" style=\"max-width:560px;width:100%\"></video><br><a href=\"${attr(src)}\">${text(v.substringAfterLast('/'))}</a>", html = true)
            }
            art.screenshots.forEach { s ->
                val src = link(s)
                msg(start, "INFO", "<a href=\"${attr(src)}\"><img src=\"${attr(src)}\" style=\"max-width:400px;border:1px solid #ccc\"></a><br>${text(s.substringAfterLast('/'))}", html = true)
            }
            val links = listOfNotNull(art.logcat?.let { "logcat" to it }, art.instrumentation?.let { "instrumentation output" to it }) +
                art.files.map { it.substringAfterLast('/') to it }
            if (links.isNotEmpty()) {
                msg(start, "INFO", "Artifacts: " + links.joinToString(" | ") { (n, p) -> "<a href=\"${attr(link(p))}\">${text(n)}</a>" }, html = true)
            }
            a.stackTrace?.takeIf { a.status != Status.PASS }?.let {
                msg(start + a.durationMs, "INFO", "<pre>${text(it)}</pre>", html = true)
            }
            when (a.status) {
                Status.FAIL, Status.ERROR -> msg(start + a.durationMs, "FAIL", message(a) ?: "FAIL")
                Status.SKIP -> msg(start + a.durationMs, "SKIP", message(a) ?: "Skipped")
                Status.PASS -> {}
            }
            sb.append("<arg>${text(t.id.fullName)}</arg>\n")
            element("doc", "Runs one instrumented test with `am instrument -e class` and collects its evidence.")
            status(robotStatus(a.status), start, start + a.durationMs, if (legacy) null else message(a))
            sb.append("</kw>\n")
        }

        private fun metadata() {
            val d = info.device
            listOfNotNull(
                "Device" to "${d.manufacturer} ${d.model}".trim(),
                "Serial" to d.serial,
                "Android" to "${d.androidRelease} (SDK ${d.sdk})",
                info.appPackage?.let { "App package" to it },
                "Instrumentation" to info.instrumentation,
                info.instrumentationArgs.takeIf { it.isNotEmpty() }?.let { "Instrumentation args" to it.entries.joinToString(", ") { (k, v) -> "$k=$v" } },
                info.selection.takeIf { it.isNotEmpty() }?.let { "Selection" to it.joinToString(" ") },
                "Command" to info.command,
                "Host" to info.host,
                "ATR report" to "[${link("report.html")}|report.html]",
            ).forEach { (k, v) -> sb.append("<meta name=\"${attr(k)}\">${text(v)}</meta>\n") }
        }

        private fun msg(at: Long, level: String, message: String, html: Boolean = false) {
            val timeAttr = if (legacy) "timestamp" else "time"
            sb.append("<msg $timeAttr=\"${time(at)}\" level=\"$level\"${if (html) " html=\"true\"" else ""}>${text(message)}</msg>\n")
        }

        private fun status(status: String, start: Long, end: Long, message: String? = null) {
            val times = if (legacy) "starttime=\"${time(start)}\" endtime=\"${time(end)}\""
            else "start=\"${time(start)}\" elapsed=\"${"%.6f".format(java.util.Locale.ROOT, (end - start).coerceAtLeast(0) / 1000.0)}\""
            sb.append("<status status=\"$status\" $times")
            if (message.isNullOrEmpty()) sb.append("/>\n") else sb.append(">${text(message)}</status>\n")
        }

        private fun element(name: String, value: String) = sb.append("<$name>${text(value)}</$name>\n")

        /** Path of an output-dir-relative artifact as seen from the Robot XML's folder. */
        private fun link(relative: String): String =
            File(outputDir, relative).relativeToOrNull(xmlDir)?.invariantSeparatorsPath ?: File(outputDir, relative).toURI().toString()

        private fun device() = "${info.device.model} (${info.device.serial}), Android ${info.device.androidRelease}"
        private fun end(t: TestResult) = t.finalAttempt.startedAt + t.finalAttempt.durationMs

        private fun message(a: Attempt): String? = when (a.status) {
            Status.ERROR -> "ERROR: " + (a.message ?: "test did not complete")
            else -> a.message
        }

        private fun time(ms: Long): String = Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()).let {
            if (legacy) LEGACY_TIME.format(it) else TIME.format(it)
        }
    }

    /** Robot has no ERROR status for tests: errors are failures with an "ERROR:" message. */
    private fun robotStatus(s: Status) = when (s) {
        Status.PASS -> "PASS"
        Status.FAIL, Status.ERROR -> "FAIL"
        Status.SKIP -> "SKIP"
    }

    private fun statusOf(tests: List<TestResult>): String = when {
        tests.any { it.status == Status.FAIL || it.status == Status.ERROR } -> "FAIL"
        tests.any { it.status == Status.PASS } -> "PASS"
        else -> "SKIP"
    }

    private val TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSS")
    private val LEGACY_TIME = DateTimeFormatter.ofPattern("yyyyMMdd HH:mm:ss.SSS")
    private fun attr(s: String) = JUnitXmlReport.attr(s)
    private fun text(s: String) = JUnitXmlReport.text(s)
}

package atr

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object JUnitXmlReport {
    fun write(result: RunResult, file: File) {
        file.parentFile.mkdirs()
        file.writeText(render(result))
    }

    fun render(result: RunResult): String {
        val s = result.stats
        val sb = StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
        val total = (result.info.finishedAt - result.info.startedAt) / 1000.0
        sb.append("<testsuites name=\"${attr(result.info.name)}\" tests=\"${s.total}\" failures=\"${s.failed}\" errors=\"${s.errors}\" skipped=\"${s.skipped}\" time=\"${sec(total)}\">\n")
        result.tests.groupBy { it.id.className }.forEach { (cls, tests) ->
            val st = Stats.of(tests)
            sb.append("  <testsuite name=\"${attr(cls)}\" tests=\"${st.total}\" failures=\"${st.failed}\" errors=\"${st.errors}\" skipped=\"${st.skipped}\"")
            sb.append(" time=\"${sec(tests.sumOf { it.durationMs } / 1000.0)}\" timestamp=\"${iso(tests.first().attempts.first().startedAt)}\" hostname=\"${attr(result.info.host)}\">\n")
            sb.append("    <properties>\n")
            sb.append("      <property name=\"device\" value=\"${attr("${result.info.device.model} (${result.info.device.serial}) Android ${result.info.device.androidRelease}")}\"/>\n")
            sb.append("      <property name=\"instrumentation\" value=\"${attr(result.info.instrumentation)}\"/>\n")
            sb.append("    </properties>\n")
            tests.forEach { t ->
                val last = t.finalAttempt
                sb.append("    <testcase name=\"${attr(t.id.method)}\" classname=\"${attr(cls)}\" time=\"${sec(t.durationMs / 1000.0)}\">\n")
                when (t.status) {
                    Status.FAIL -> sb.append("      <failure message=\"${attr(last.message ?: "")}\" type=\"${attr(type(last))}\">${text(last.stackTrace ?: last.message ?: "")}</failure>\n")
                    Status.ERROR -> sb.append("      <error message=\"${attr(last.message ?: "")}\" type=\"${attr(type(last))}\">${text(last.stackTrace ?: last.message ?: "")}</error>\n")
                    Status.SKIP -> sb.append("      <skipped message=\"${attr(last.message ?: "")}\"/>\n")
                    Status.PASS -> {}
                }
                val attachments = t.attempts.flatMap { a -> listOfNotNull(a.artifacts.video, a.artifacts.logcat) + a.artifacts.screenshots }
                if (attachments.isNotEmpty() || t.flaky) {
                    sb.append("      <system-out>")
                    if (t.flaky) sb.append(text("FLAKY: passed on attempt ${t.attempts.size}\n"))
                    attachments.forEach { sb.append(text("[[ATTACHMENT|$it]]\n")) }
                    sb.append("</system-out>\n")
                }
                sb.append("    </testcase>\n")
            }
            sb.append("  </testsuite>\n")
        }
        sb.append("</testsuites>\n")
        return sb.toString()
    }

    private fun type(a: Attempt) = a.message?.substringBefore(':')?.takeIf { it.isNotBlank() && !it.contains(' ') } ?: a.status.name
    private fun sec(v: Double) = "%.3f".format(java.util.Locale.ROOT, v)
    private fun iso(ms: Long) = DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()))
    private fun clean(s: String) = s.filter { it == '\t' || it == '\n' || it == '\r' || it >= ' ' }
    fun attr(s: String) = clean(s).replace("&", "&amp;").replace("\"", "&quot;").replace("<", "&lt;").replace(">", "&gt;").replace("\n", "&#10;")
    fun text(s: String) = clean(s).replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
}

object HtmlReport {
    private const val MAX_LOG_LINES = 500

    fun write(result: RunResult, outputDir: File, file: File = File(outputDir, "report.html")) {
        file.parentFile.mkdirs()
        val template = resource("report/template.html")
        val data = JsonReport.json.encodeToString(RunResult.serializer(), result)
        val extras = buildJsonObject {
            put("logcat", JsonObject(result.tests.flatMap { it.attempts }.mapNotNull { a ->
                a.artifacts.logcat?.let { path ->
                    File(outputDir, path).takeIf { it.isFile }?.let { path to JsonPrimitive(excerpt(it, result.info.appPackage)) }
                }
            }.toMap()))
        }
        val html = template
            .replace("{{TITLE}}", JUnitXmlReport.text(result.info.name))
            .replace("/*{{CSS}}*/", resource("report/report.css"))
            .replace("/*{{JS}}*/", resource("report/report.js"))
            .replace("{{DATA}}", scriptSafe(data))
            .replace("{{EXTRAS}}", scriptSafe(JsonReport.json.encodeToString(JsonObject.serializer(), extras)))
        file.writeText(html)
    }

    /** Warnings, errors, crashes, the test runner and the app's own lines; at most [MAX_LOG_LINES]. */
    fun excerpt(logcat: File, appPackage: String?): String {
        val keep = Regex("""\s[EWF]\s|TestRunner|AndroidRuntime|ComposeTest|Instrumentation""")
        val lines = logcat.readLines().filter { line ->
            keep.containsMatchIn(line) || (appPackage != null && line.contains(appPackage))
        }
        return lines.takeLast(MAX_LOG_LINES).joinToString("\n")
    }

    private fun scriptSafe(json: String) = json.replace("</", "<\\/").replace("<!--", "<\\!--")

    private fun resource(path: String): String =
        HtmlReport::class.java.getResourceAsStream("/$path")?.bufferedReader()?.use { it.readText() }
            ?: error("Missing resource $path")
}

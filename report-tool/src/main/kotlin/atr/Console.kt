package atr

import java.io.PrintStream

/** Robot Framework–style console output. */
class Console(private val out: PrintStream = System.out, private val width: Int = 78) {
    private val ansi = System.console() != null && System.getenv("NO_COLOR") == null

    private fun color(s: String, code: String) = if (ansi) "\u001B[${code}m$s\u001B[0m" else s

    fun rule(c: Char = '=') = out.println(c.toString().repeat(width))
    fun line(s: String = "") = out.println(s)
    fun info(s: String) = out.println("[ ${color("INFO", "36")} ] $s")
    fun warn(s: String) = out.println("[ ${color("WARN", "33")} ] $s")

    fun header(title: String, subtitle: String) {
        rule('=')
        line(title)
        line(color(subtitle, "90"))
        rule('=')
    }

    fun suite(name: String) {
        line(name)
        rule('-')
    }

    fun badge(status: Status): String = when (status) {
        Status.PASS -> color("PASS", "32")
        Status.FAIL -> color("FAIL", "31")
        Status.ERROR -> color("ERROR", "31")
        Status.SKIP -> color("SKIP", "33")
    }

    /** `test name ......... | PASS | 12.3s` */
    fun test(name: String, status: Status, durationMs: Long, note: String? = null) {
        val tail = "| ${badge(status).padEnd(badge(status).length + 5 - status.name.length)} | ${"%6.1fs".format(durationMs / 1000.0)}"
        val visibleTail = "| ${status.name.padEnd(5)} | ${"%6.1fs".format(durationMs / 1000.0)}"
        val room = width - visibleTail.length - 1
        val label = if (name.length > room) name.take(room - 3) + "..." else name.padEnd(room)
        line("$label $tail")
        note?.let { line("    ${color(it, "90")}") }
    }

    fun summary(stats: Stats, durationMs: Long) {
        rule('=')
        val parts = listOf(
            "${stats.total} tests",
            color("${stats.passed} passed", "32"),
            color("${stats.failed} failed", if (stats.failed > 0) "31" else "90"),
            color("${stats.errors} errors", if (stats.errors > 0) "31" else "90"),
            "${stats.skipped} skipped",
        ) + (if (stats.flaky > 0) listOf(color("${stats.flaky} flaky", "33")) else emptyList())
        line(parts.joinToString(", ") + "   (${formatDuration(durationMs)})")
        rule('=')
    }

    companion object {
        fun formatDuration(ms: Long): String {
            val s = ms / 1000
            return if (s >= 60) "%dm %02ds".format(s / 60, s % 60) else "%.1fs".format(ms / 1000.0)
        }
    }
}

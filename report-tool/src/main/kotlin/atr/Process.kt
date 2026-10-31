package atr

import java.io.File
import java.io.InputStream
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

data class ExecResult(val exitCode: Int, val stdout: String, val stderr: String, val timedOut: Boolean) {
    val ok: Boolean get() = exitCode == 0 && !timedOut
    val output: String get() = if (stderr.isBlank()) stdout else "$stdout\n$stderr"
}

/** Runs external processes with output capture and a hard timeout. */
object Processes {
    fun exec(
        command: List<String>,
        timeoutMs: Long = 60_000,
        env: Map<String, String> = emptyMap(),
        workDir: File? = null,
    ): ExecResult {
        val builder = ProcessBuilder(command).apply {
            workDir?.let { directory(it) }
            environment().putAll(env)
        }
        val process = try {
            builder.start()
        } catch (e: Exception) {
            return ExecResult(-1, "", "Cannot start ${command.first()}: ${e.message}", false)
        }
        val out = StringBuilder()
        val err = StringBuilder()
        val tOut = pump(process.inputStream, out)
        val tErr = pump(process.errorStream, err)
        val finished = process.waitFor(timeoutMs, TimeUnit.MILLISECONDS)
        if (!finished) process.destroyForcibly().waitFor(5, TimeUnit.SECONDS)
        tOut.join(2_000)
        tErr.join(2_000)
        return ExecResult(if (finished) process.exitValue() else -1, out.toString(), err.toString(), !finished)
    }

    private fun pump(stream: InputStream, into: StringBuilder) = thread(isDaemon = true) {
        stream.bufferedReader().useLines { lines -> lines.forEach { synchronized(into) { into.appendLine(it) } } }
    }

    /** Finds an executable on PATH (adds .exe/.bat on Windows), or returns null. */
    fun which(name: String): String? {
        val candidate = File(name)
        if (candidate.isAbsolute || name.contains('/') || name.contains('\\')) {
            return candidate.takeIf { it.isFile }?.absolutePath
        }
        val exts = if (isWindows) listOf(".exe", ".bat", ".cmd", "") else listOf("")
        return System.getenv("PATH").orEmpty().split(File.pathSeparatorChar)
            .flatMap { dir -> exts.map { File(dir, name + it) } }
            .firstOrNull { it.isFile }?.absolutePath
    }

    val isWindows: Boolean = System.getProperty("os.name").lowercase().contains("win")
}

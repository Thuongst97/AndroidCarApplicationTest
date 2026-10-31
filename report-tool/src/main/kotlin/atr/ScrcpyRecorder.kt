package atr

import java.io.File
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

/**
 * Records the device screen with scrcpy (host side). Works on devices without a hardware video
 * encoder when the config passes a software encoder (e.g. `--video-encoder=c2.android.avc.encoder`).
 *
 * [start] returns only after scrcpy reports "Recording started", so the test that follows is fully
 * captured. [Session.stop] ends scrcpy's device-side server, which makes scrcpy finalise the MP4.
 */
class ScrcpyRecorder(
    private val executable: String,
    private val adb: Adb,
    private val extraArgs: List<String>,
) {
    inner class Session(private val process: Process, val file: File, private val log: StringBuffer) {
        fun stop(): Boolean {
            adb.shell("pkill -f com.genymobile.scrcpy.Server", 10_000)
            if (!process.waitFor(20, TimeUnit.SECONDS)) {
                process.destroy()
                if (!process.waitFor(5, TimeUnit.SECONDS)) process.destroyForcibly()
            }
            return file.isFile && file.length() > 0
        }

        val output: String get() = log.toString()
    }

    fun start(file: File, timeoutMs: Long = 30_000): Session {
        file.parentFile.mkdirs()
        file.delete()
        val command = listOf(executable, "-s", adb.serial, "--no-window", "--no-audio", "--no-control") +
            extraArgs + "--record=${file.absolutePath}"
        val process = ProcessBuilder(command)
            .apply { environment()["ADB"] = adb.executable }
            .redirectErrorStream(true)
            .start()
        val log = StringBuffer()
        val started = java.util.concurrent.CountDownLatch(1)
        thread(isDaemon = true) {
            process.inputStream.bufferedReader().useLines { lines ->
                lines.forEach {
                    log.appendLine(it)
                    if (it.contains("Recording started")) started.countDown()
                }
            }
        }
        if (!started.await(timeoutMs, TimeUnit.MILLISECONDS)) {
            process.destroyForcibly()
            throw IllegalStateException("scrcpy did not start recording:\n$log")
        }
        return Session(process, file, log)
    }

    companion object {
        /** scrcpy executable, or null when it is not installed. */
        fun locate(configured: String): String? = Processes.which(configured)
    }
}

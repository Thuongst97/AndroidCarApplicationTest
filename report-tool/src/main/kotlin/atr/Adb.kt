package atr

import java.io.File

/** Thin wrapper around the `adb` executable for one device. */
class Adb(val executable: String, val serial: String) {

    fun exec(args: List<String>, timeoutMs: Long = 120_000): ExecResult =
        Processes.exec(listOf(executable, "-s", serial) + args, timeoutMs)

    fun shell(command: String, timeoutMs: Long = 60_000): ExecResult = exec(listOf("shell", command), timeoutMs)

    fun prop(name: String): String = shell("getprop $name").stdout.trim()

    fun deviceInfo() = DeviceInfo(
        serial = serial,
        manufacturer = prop("ro.product.manufacturer"),
        model = prop("ro.product.model"),
        androidRelease = prop("ro.build.version.release"),
        sdk = prop("ro.build.version.sdk"),
    )

    fun install(apk: File): ExecResult = exec(listOf("install", "-r", "-t", "-g", apk.absolutePath), timeoutMs = 600_000)

    /** Lines like `instrumentation:com.app.test/androidx.test.runner.AndroidJUnitRunner (target=com.app)`. */
    fun instrumentations(): List<Instrumentation> =
        shell("pm list instrumentation").stdout.lines().mapNotNull { Instrumentation.parse(it) }

    fun clearLogcat() = exec(listOf("logcat", "-c"), 15_000)
    fun dumpLogcat(): String = exec(listOf("logcat", "-d", "-v", "threadtime"), 60_000).stdout

    fun pull(remote: String, local: File): ExecResult {
        local.mkdirs()
        return exec(listOf("pull", remote, local.absolutePath), 300_000)
    }

    fun forceStop(pkg: String) = shell("am force-stop $pkg", 15_000)

    companion object {
        /** adb from ANDROID_HOME / ANDROID_SDK_ROOT, else PATH. */
        fun locate(): String? {
            val exe = if (Processes.isWindows) "adb.exe" else "adb"
            listOf("ANDROID_HOME", "ANDROID_SDK_ROOT").mapNotNull { System.getenv(it) }.forEach { sdk ->
                File(sdk, "platform-tools/$exe").takeIf { it.isFile }?.let { return it.absolutePath }
            }
            return Processes.which("adb")
        }

        /** Serials of devices in state "device". */
        fun connectedDevices(adb: String): List<String> =
            Processes.exec(listOf(adb, "devices"), 30_000).stdout.lines().drop(1)
                .map { it.trim().split(Regex("\\s+")) }
                .filter { it.size >= 2 && it[1] == "device" }
                .map { it[0] }
    }
}

data class Instrumentation(val testPackage: String, val runner: String, val targetPackage: String) {
    val component: String get() = "$testPackage/$runner"

    companion object {
        private val pattern = Regex("""instrumentation:([^/\s]+)/(\S+)\s+\(target=([^)]+)\)""")
        fun parse(line: String): Instrumentation? =
            pattern.find(line.trim())?.destructured?.let { (pkg, runner, target) -> Instrumentation(pkg, runner, target) }
    }
}

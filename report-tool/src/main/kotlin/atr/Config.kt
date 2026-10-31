package atr

import org.yaml.snakeyaml.Yaml
import java.io.File

enum class VideoMode {
    ALWAYS, ON_FAILURE, NEVER;

    companion object {
        fun parse(s: String): VideoMode = when (s.trim().lowercase().replace('_', '-')) {
            "always", "on", "true" -> ALWAYS
            "on-failure", "onfailure", "failure" -> ON_FAILURE
            "never", "off", "false" -> NEVER
            else -> throw UsageException("Invalid video mode '$s' (use always | on-failure | never)")
        }
    }
}

/** Effective settings: atr.yml (if any) overridden by command-line options. */
data class Config(
    val name: String = "Android UI tests",
    val serial: String? = null,
    val appApk: File? = null,
    val testApk: File? = null,
    val install: Boolean = true,
    /** `testPackage/runner`, or null to auto-detect from `pm list instrumentation`. */
    val instrumentation: String? = null,
    val appPackage: String? = null,
    val instrumentationArgs: Map<String, String> = emptyMap(),
    /** Tag alias → annotation FQN, e.g. Smoke → com.app.Smoke. */
    val tags: Map<String, String> = emptyMap(),
    val retries: Int = 0,
    val timeoutSeconds: Int = 300,
    val video: VideoMode = VideoMode.ALWAYS,
    val logcat: Boolean = true,
    val pullPaths: List<String> = emptyList(),
    val scrcpyPath: String = "scrcpy",
    val scrcpyArgs: List<String> = listOf("--video-codec=h264", "--max-fps=15", "--video-bit-rate=2M"),
    val outputDir: File = File("atr-results"),
    /** Also write a Robot Framework output.xml here (for rebot). */
    val robotOutput: File? = null,
    /** Robot Framework 6 format (schema 4) instead of 7 (schema 5). */
    val robotLegacy: Boolean = false,
) {
    /** Resolves a tag given on the command line to an annotation class name. */
    fun annotationFor(tag: String): String =
        tags.entries.firstOrNull { it.key.equals(tag, ignoreCase = true) }?.value
            ?: if (tag.contains('.')) tag else throw UsageException("Unknown tag '$tag'. Define it under 'tags:' in the config or use the annotation's full class name.")

    companion object {
        const val DEFAULT_FILE = "atr.yml"

        fun load(file: File): Config {
            if (!file.isFile) throw UsageException("Config file not found: ${file.absolutePath}")
            val base = file.absoluteFile.parentFile
            val root: Map<String, Any?> = file.reader().use { Yaml().load<Map<String, Any?>>(it) } ?: emptyMap()
            fun section(name: String): Map<String, Any?> = (root[name] as? Map<*, *>)?.entries
                ?.associate { it.key.toString() to it.value } ?: emptyMap()
            fun path(value: Any?): File? = value?.toString()?.takeIf { it.isNotBlank() }?.let { p ->
                File(p).let { if (it.isAbsolute) it else File(base, p) }.normalize()
            }
            fun stringMap(value: Any?): Map<String, String> =
                (value as? Map<*, *>)?.entries?.associate { it.key.toString() to it.value.toString() } ?: emptyMap()
            fun stringList(value: Any?): List<String>? = (value as? List<*>)?.map { it.toString() }

            val device = section("device")
            val apks = section("apks")
            val execution = section("execution")
            val artifacts = section("artifacts")
            val scrcpy = section("scrcpy")
            val output = section("output")
            val defaults = Config()
            return Config(
                name = root["name"]?.toString() ?: defaults.name,
                serial = device["serial"]?.toString(),
                appApk = path(apks["app"]),
                testApk = path(apks["test"]),
                install = root["install"] as? Boolean ?: true,
                instrumentation = root["instrumentation"]?.toString()?.takeUnless { it.equals("auto", true) },
                appPackage = root["appPackage"]?.toString(),
                instrumentationArgs = stringMap(root["instrumentationArgs"]),
                tags = stringMap(root["tags"]),
                retries = (execution["retries"] as? Number)?.toInt() ?: defaults.retries,
                timeoutSeconds = (execution["timeoutSeconds"] as? Number)?.toInt() ?: defaults.timeoutSeconds,
                video = artifacts["video"]?.toString()?.let(VideoMode::parse) ?: defaults.video,
                logcat = artifacts["logcat"] as? Boolean ?: true,
                pullPaths = stringList(artifacts["pullPaths"]) ?: emptyList(),
                scrcpyPath = scrcpy["path"]?.toString() ?: defaults.scrcpyPath,
                scrcpyArgs = stringList(scrcpy["args"]) ?: defaults.scrcpyArgs,
                outputDir = path(output["dir"]) ?: File(base, "atr-results"),
                robotOutput = path(output["robot"]),
                robotLegacy = output["robotLegacy"] as? Boolean ?: false,
            )
        }
    }
}

/** Invalid command line or configuration: exit code 252. */
class UsageException(message: String) : RuntimeException(message)

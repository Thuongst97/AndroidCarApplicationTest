package atr

import java.io.File

/** Parsed command line. */
data class CliArgs(
    val command: String,
    val config: File? = null,
    val tests: List<String> = emptyList(),
    val suites: List<String> = emptyList(),
    val includes: List<String> = emptyList(),
    val excludes: List<String> = emptyList(),
    val rerunFailed: File? = null,
    val serial: String? = null,
    val retries: Int? = null,
    val timeoutSeconds: Int? = null,
    val video: VideoMode? = null,
    val args: Map<String, String> = emptyMap(),
    val noInstall: Boolean = false,
    val outputDir: File? = null,
    val name: String? = null,
    val input: File? = null,
    val robot: File? = null,
    val robotLegacy: Boolean = false,
    val positional: List<String> = emptyList(),
)

object Cli {
    val commands = setOf("run", "list", "report", "init", "version", "help")

    fun parse(argv: Array<String>): CliArgs {
        if (argv.isEmpty()) return CliArgs("help")
        val first = argv[0]
        val command = when {
            first in commands -> first
            first == "--help" || first == "-h" -> "help"
            first == "--version" -> "version"
            else -> throw UsageException("Unknown command '$first'. Try: atr help")
        }
        var a = CliArgs(command)
        var i = 1
        fun value(opt: String): String {
            if (i + 1 >= argv.size) throw UsageException("Option $opt needs a value")
            i++
            return argv[i]
        }
        fun int(opt: String): Int = value(opt).toIntOrNull()?.takeIf { it >= 0 }
            ?: throw UsageException("Option $opt needs a non-negative number")

        while (i < argv.size) {
            val opt = argv[i]
            // Support --opt=value as well as --opt value
            val (name, inline) = if (opt.startsWith("--") && opt.contains('=')) opt.substringBefore('=') to opt.substringAfter('=') else opt to null
            fun v(): String = inline ?: value(name)
            a = when (name) {
                "-c", "--config" -> a.copy(config = File(v()))
                "-t", "--test" -> a.copy(tests = a.tests + v())
                "-s", "--suite" -> a.copy(suites = a.suites + v())
                "-i", "--include" -> a.copy(includes = a.includes + v())
                "-e", "--exclude" -> a.copy(excludes = a.excludes + v())
                "-R", "--rerunfailed" -> a.copy(rerunFailed = File(v()))
                "--serial" -> a.copy(serial = v())
                "--retries" -> a.copy(retries = inline?.toIntOrNull() ?: int(name))
                "--timeout" -> a.copy(timeoutSeconds = inline?.toIntOrNull() ?: int(name))
                "--video" -> a.copy(video = VideoMode.parse(v()))
                "-a", "--arg" -> {
                    val kv = v()
                    if (!kv.contains('=')) throw UsageException("--arg expects KEY=VALUE, got '$kv'")
                    a.copy(args = a.args + (kv.substringBefore('=') to kv.substringAfter('=')))
                }
                "--no-install" -> a.copy(noInstall = true)
                "-d", "--outputdir" -> a.copy(outputDir = File(v()))
                "-N", "--name" -> a.copy(name = v())
                "-I", "--input" -> a.copy(input = File(v()))
                "--robot" -> a.copy(robot = File(v()))
                "--robot-legacy" -> a.copy(robotLegacy = true)
                "-h", "--help" -> a.copy(command = "help")
                else -> if (opt.startsWith("-")) throw UsageException("Unknown option '$opt'") else a.copy(positional = a.positional + opt)
            }
            i++
        }
        return a
    }

    /** Loads the config file (explicit, or ./atr.yml if present) and applies CLI overrides. */
    fun effectiveConfig(a: CliArgs): Config {
        val file = a.config ?: File(Config.DEFAULT_FILE).takeIf { it.isFile }
        var c = file?.let(Config::load) ?: Config()
        a.name?.let { c = c.copy(name = it) }
        a.serial?.let { c = c.copy(serial = it) }
        a.retries?.let { c = c.copy(retries = it) }
        a.timeoutSeconds?.let { c = c.copy(timeoutSeconds = it) }
        a.video?.let { c = c.copy(video = it) }
        if (a.noInstall) c = c.copy(install = false)
        a.outputDir?.let { c = c.copy(outputDir = it.absoluteFile) }
        a.robot?.let { c = c.copy(robotOutput = it.absoluteFile) }
        if (a.robotLegacy) c = c.copy(robotLegacy = true)
        if (a.args.isNotEmpty()) c = c.copy(instrumentationArgs = c.instrumentationArgs + a.args)
        return c
    }

    val help = """
        ATR - Android Test Runner ${Version.current}
        Runs Android instrumented tests one by one with per-test video, screenshots and logcat,
        and writes report.html, output.json and junit.xml.

        Usage:
          atr run    [options]           run tests
          atr list   [options]           list the tests that would run (dry run)
          atr report -I output.json [-d DIR] [--robot FILE]
                                         regenerate report.html, junit.xml (and Robot XML)
          atr init   [atr.yml]           write a commented configuration template
          atr version

        Selection (repeatable; like Robot Framework):
          -t, --test PATTERN         method, Class#method or pkg.Class#method (* and ? allowed)
          -s, --suite PATTERN        test class, simple or fully-qualified name
          -i, --include TAG          only tests annotated with TAG (alias from config or FQN)
          -e, --exclude TAG          skip tests annotated with TAG
          -R, --rerunfailed FILE     only tests that failed in a previous output.json

        Execution:
          -c, --config FILE          configuration file (default: ./atr.yml if present)
              --serial SERIAL        device serial (default: config, or the only connected device)
              --retries N            extra attempts for failing tests
              --timeout SECONDS      per-test timeout
              --video MODE           always | on-failure | never
          -a, --arg KEY=VALUE        extra instrumentation argument (repeatable)
              --no-install           do not install the APKs
          -d, --outputdir DIR        results folder
          -N, --name NAME            run name shown in the report

        Robot Framework (rebot):
              --robot FILE           also write a Robot Framework output.xml (merge: rebot a.xml FILE)
              --robot-legacy         Robot Framework 6 format instead of 7

        Exit code: 0 all passed, 1-250 number of failed tests, 252 invalid usage, 255 error
    """.trimIndent()
}

object Version {
    val current: String by lazy {
        Version::class.java.getResourceAsStream("/atr-version.properties")?.use { s ->
            java.util.Properties().apply { load(s) }.getProperty("version")
        }?.takeUnless { it.contains("\${") } ?: "dev"
    }
}

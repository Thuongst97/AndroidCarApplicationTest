package atr

import java.io.File
import kotlin.system.exitProcess

const val EXIT_USAGE = 252
const val EXIT_ERROR = 255

fun main(argv: Array<String>) {
    exitProcess(App(argv).run())
}

class App(private val argv: Array<String>, private val console: Console = Console()) {

    fun run(): Int = try {
        val args = Cli.parse(argv)
        when (args.command) {
            "help" -> { console.line(Cli.help); 0 }
            "version" -> { console.line("ATR ${Version.current}"); 0 }
            "init" -> init(args)
            "report" -> report(args)
            "list" -> list(args)
            "run" -> runTests(args)
            else -> throw UsageException("Unknown command ${args.command}")
        }
    } catch (e: UsageException) {
        System.err.println("[ ERROR ] ${e.message}")
        EXIT_USAGE
    } catch (e: Exception) {
        System.err.println("[ ERROR ] ${e.message}")
        if (System.getenv("ATR_DEBUG") != null) e.printStackTrace()
        EXIT_ERROR
    }

    private fun select(args: CliArgs, config: Config, runner: Runner, prepared: Runner.Prepared): Pair<List<TestId>, Selection> {
        val selection = Selection(args.tests, args.suites, args.rerunFailed?.let(Selection::failedIn))
        val discovered = runner.discover(prepared, args.includes, args.excludes)
        // Group by suite while keeping the runner's order inside each class.
        val order = discovered.map { it.className }.distinct()
        val selected = selection.apply(discovered).sortedBy { order.indexOf(it.className) }
        if (selected.isEmpty()) {
            throw UsageException("No tests match the selection (${describe(args, selection).joinToString(" ").ifBlank { "all" }}); ${discovered.size} tests were discovered.")
        }
        return selected to selection
    }

    private fun describe(args: CliArgs, selection: Selection): List<String> =
        selection.describe() + args.includes.map { "--include $it" } + args.excludes.map { "--exclude $it" }

    private fun list(args: CliArgs): Int {
        val config = Cli.effectiveConfig(args)
        val runner = Runner(config, console)
        val prepared = runner.prepare()
        val (tests, _) = select(args, config, runner, prepared)
        tests.groupBy { it.className }.forEach { (cls, ts) ->
            console.line(cls)
            ts.forEach { console.line("    ${it.method}") }
        }
        console.line("${tests.size} test(s) selected.")
        return 0
    }

    private fun runTests(args: CliArgs): Int {
        val config = Cli.effectiveConfig(args)
        val runner = Runner(config, console)
        val prepared = runner.prepare()
        val (tests, selection) = select(args, config, runner, prepared)

        // Fresh results folder (artifacts of a previous run would confuse the report).
        File(config.outputDir, "artifacts").deleteRecursively()
        val command = "atr " + argv.joinToString(" ") { if (it.contains(' ')) "\"$it\"" else it }
        val result = runner.execute(prepared, tests, describe(args, selection), command)
        writeReports(result, config.outputDir)
        config.robotOutput?.let { RobotXmlReport.write(result, config.outputDir, it, config.robotLegacy) }
        console.summary(result.stats, result.info.finishedAt - result.info.startedAt)
        console.line("Output:  ${File(config.outputDir, "output.json").absolutePath}")
        console.line("JUnit:   ${File(config.outputDir, "junit.xml").absolutePath}")
        console.line("Report:  ${File(config.outputDir, "report.html").absolutePath}")
        config.robotOutput?.let { console.line("Robot:   ${it.absolutePath}  (merge with: rebot ... ${it.name})") }
        return result.stats.notPassed.coerceAtMost(250)
    }

    private fun report(args: CliArgs): Int {
        val input = args.input ?: args.positional.firstOrNull()?.let(::File)
            ?: throw UsageException("atr report needs --input output.json")
        if (!input.isFile) throw UsageException("Not found: ${input.absolutePath}")
        val result = JsonReport.read(input)
        val outDir = args.outputDir?.absoluteFile ?: input.absoluteFile.parentFile
        HtmlReport.write(result, outDir)
        JUnitXmlReport.write(result, File(outDir, "junit.xml"))
        console.line("Report:  ${File(outDir, "report.html").absolutePath}")
        args.robot?.let {
            RobotXmlReport.write(result, outDir, it, args.robotLegacy)
            console.line("Robot:   ${it.absoluteFile}")
        }
        return 0
    }

    private fun init(args: CliArgs): Int {
        val target = File(args.positional.firstOrNull() ?: Config.DEFAULT_FILE)
        if (target.exists()) throw UsageException("${target.path} already exists.")
        val template = App::class.java.getResourceAsStream("/atr.template.yml")!!.bufferedReader().use { it.readText() }
        target.writeText(template)
        console.line("Created ${target.absolutePath}. Edit the APK paths and run: atr run")
        return 0
    }

    companion object {
        fun writeReports(result: RunResult, outDir: File) {
            JsonReport.write(result, File(outDir, "output.json"))
            JUnitXmlReport.write(result, File(outDir, "junit.xml"))
            HtmlReport.write(result, outDir)
        }
    }
}

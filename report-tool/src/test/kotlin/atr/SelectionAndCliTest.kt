package atr

import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SelectionTest {
    private val all = listOf(
        TestId("com.app.DashboardScreenTest", "muteAndTheme_toggled"),
        TestId("com.app.DashboardScreenTest", "speedometer_overLimit"),
        TestId("com.app.MediaCompanionTest", "playPause_tapped"),
        TestId("com.app.LuxuryClockTest", "swipe_showsPages"),
    )

    @Test
    fun testPattern_matchesMethodClassAndFullName_caseInsensitive() {
        assertEquals(1, Selection(tests = listOf("MUTEANDTHEME_toggled")).apply(all).size)
        assertEquals(1, Selection(tests = listOf("DashboardScreenTest#speedometer*")).apply(all).size)
        assertEquals(1, Selection(tests = listOf("com.app.MediaCompanionTest#playPause_tapped")).apply(all).size)
        assertEquals(2, Selection(tests = listOf("*_tapped", "*mute*")).apply(all).size)
        assertEquals(1, Selection(tests = listOf("swipe_shows?ages")).apply(all).size)
    }

    @Test
    fun suitePattern_matchesSimpleAndFullClassName() {
        assertEquals(2, Selection(suites = listOf("DashboardScreenTest")).apply(all).size)
        assertEquals(4, Selection(suites = listOf("com.app.*Test")).apply(all).size)
        assertEquals(2, Selection(suites = listOf("*Dashboard*")).apply(all).size)
    }

    @Test
    fun suiteAndTest_mustBothMatch() {
        val s = Selection(tests = listOf("*_tapped", "*mute*"), suites = listOf("Dashboard*"))
        assertEquals(listOf("muteAndTheme_toggled"), s.apply(all).map { it.method })
    }

    @Test
    fun noPatterns_selectsAll() = assertEquals(all, Selection().apply(all))

    @Test
    fun rerunFailed_readsPreviousOutput() {
        val dir = Files.createTempDirectory("atr").toFile()
        val file = File(dir, "output.json")
        JsonReport.write(Fixtures.result(), file)
        val ids = Selection.failedIn(file)
        assertEquals(setOf("com.app.DashboardScreenTest#speedometer_overLimit", "com.app.MediaCompanionTest#crash"), ids)
        assertTrue(Selection(onlyIds = ids).matches(TestId("com.app.MediaCompanionTest", "crash")))
        assertFalse(Selection(onlyIds = ids).matches(TestId("com.app.DashboardScreenTest", "muteAndTheme_toggled")))
    }
}

class CliTest {
    @Test
    fun parsesRobotStyleOptions() {
        val a = Cli.parse(arrayOf("run", "-t", "*mute*", "--test=Foo#bar", "-s", "Dashboard*", "-i", "Smoke", "-e", "Slow",
            "--retries", "2", "--timeout", "90", "--video", "on-failure", "-a", "observeStepMs=1500", "--no-install", "-d", "out", "-N", "Nightly"))
        assertEquals("run", a.command)
        assertEquals(listOf("*mute*", "Foo#bar"), a.tests)
        assertEquals(listOf("Dashboard*"), a.suites)
        assertEquals(listOf("Smoke"), a.includes)
        assertEquals(listOf("Slow"), a.excludes)
        assertEquals(2, a.retries)
        assertEquals(90, a.timeoutSeconds)
        assertEquals(VideoMode.ON_FAILURE, a.video)
        assertEquals(mapOf("observeStepMs" to "1500"), a.args)
        assertTrue(a.noInstall)
        assertEquals("Nightly", a.name)
    }

    @Test
    fun rejectsUnknownOptionsAndCommands() {
        assertFailsWith<UsageException> { Cli.parse(arrayOf("run", "--bogus")) }
        assertFailsWith<UsageException> { Cli.parse(arrayOf("explode")) }
        assertFailsWith<UsageException> { Cli.parse(arrayOf("run", "--arg", "novalue")) }
        assertFailsWith<UsageException> { Cli.parse(arrayOf("run", "--retries")) }
    }

    @Test
    fun noArgumentsShowsHelp() = assertEquals("help", Cli.parse(emptyArray()).command)
}

class ConfigTest {
    @Test
    fun loadsYamlAndResolvesRelativePaths() {
        val dir = Files.createTempDirectory("atr-cfg").toFile()
        val file = File(dir, "atr.yml")
        file.writeText(
            """
            name: "Pi-Dash"
            device: { serial: "192.168.1.140:35381" }
            apks:
              app: app/app.apk
              test: app/test.apk
            appPackage: com.app
            instrumentation: auto
            instrumentationArgs: { observeStepMs: 1500, evidence: true }
            tags: { Smoke: com.app.Smoke }
            execution: { retries: 1, timeoutSeconds: 120 }
            artifacts:
              video: on-failure
              pullPaths: [ /sdcard/x ]
            scrcpy: { args: [ "--max-fps=10" ] }
            output: { dir: results }
            """.trimIndent(),
        )
        val c = Config.load(file)
        assertEquals("Pi-Dash", c.name)
        assertEquals("192.168.1.140:35381", c.serial)
        assertEquals(File(dir, "app/app.apk").normalize(), c.appApk)
        assertNull(c.instrumentation)
        assertEquals(mapOf("observeStepMs" to "1500", "evidence" to "true"), c.instrumentationArgs)
        assertEquals(1, c.retries)
        assertEquals(VideoMode.ON_FAILURE, c.video)
        assertEquals(listOf("/sdcard/x"), c.pullPaths)
        assertEquals(listOf("--max-fps=10"), c.scrcpyArgs)
        assertEquals(File(dir, "results").normalize(), c.outputDir)
        assertEquals("com.app.Smoke", c.annotationFor("smoke"))
        assertEquals("org.x.Big", c.annotationFor("org.x.Big"))
        assertFailsWith<UsageException> { c.annotationFor("Unknown") }
    }

    @Test
    fun templateIsValidYaml() {
        val dir = Files.createTempDirectory("atr-tpl").toFile()
        val file = File(dir, "atr.yml")
        file.writeText(App::class.java.getResourceAsStream("/atr.template.yml")!!.bufferedReader().readText())
        val c = Config.load(file)
        assertNull(c.serial)
        assertEquals(VideoMode.ALWAYS, c.video)
    }
}

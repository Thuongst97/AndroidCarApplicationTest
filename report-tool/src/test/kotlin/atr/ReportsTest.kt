package atr

import java.io.File
import java.nio.file.Files
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

object Fixtures {
    private fun attempt(i: Int, s: Status, msg: String? = null, stack: String? = null, video: String? = null) =
        Attempt(i, s, 1_700_000_000_000 + i * 10_000L, 4_000, msg, stack, Artifacts(video = video, logcat = "artifacts/x/logcat.txt"))

    fun result() = RunResult(
        info = RunInfo(
            name = "Pi-Dash <UI> & tests", toolVersion = "test", startedAt = 1_700_000_000_000, finishedAt = 1_700_000_060_000,
            device = DeviceInfo("192.168.1.140:35381", "Raspberry", "Pi 4", "16", "36"),
            instrumentation = "com.app.test/androidx.test.runner.AndroidJUnitRunner", appPackage = "com.app",
            instrumentationArgs = mapOf("observeStepMs" to "1500"), selection = listOf("--test *"), host = "pc", command = "atr run",
        ),
        tests = listOf(
            TestResult(TestId("com.app.DashboardScreenTest", "muteAndTheme_toggled"), Status.PASS,
                listOf(attempt(1, Status.FAIL, "java.lang.AssertionError: flaky"), attempt(2, Status.PASS, video = "artifacts/a/video.mp4"))),
            TestResult(TestId("com.app.DashboardScreenTest", "speedometer_overLimit"), Status.FAIL,
                listOf(attempt(1, Status.FAIL, "java.lang.AssertionError: Expected <88>", "java.lang.AssertionError: Expected <88>\n\tat X.kt:1"))),
            TestResult(TestId("com.app.MediaCompanionTest", "crash"), Status.ERROR, listOf(attempt(1, Status.ERROR, "Process crashed."))),
            TestResult(TestId("com.app.MediaCompanionTest", "ignored"), Status.SKIP, listOf(attempt(1, Status.SKIP))),
        ),
    )
}

class ReportsTest {
    @Test
    fun stats_countFlakyAndPassRate() {
        val s = Fixtures.result().stats
        assertEquals(4, s.total); assertEquals(1, s.passed); assertEquals(1, s.failed); assertEquals(1, s.errors)
        assertEquals(1, s.skipped); assertEquals(1, s.flaky); assertEquals(2, s.notPassed)
        assertEquals(100.0 / 3, s.passRate, 0.001)
    }

    @Test
    fun json_roundTrips() {
        val f = File(Files.createTempDirectory("atr-json").toFile(), "output.json")
        JsonReport.write(Fixtures.result(), f)
        val back = JsonReport.read(f)
        assertEquals(Fixtures.result(), back)
        assertTrue(f.readText().contains("\"flaky\": true"))
    }

    @Test
    fun junitXml_isWellFormedAndComplete() {
        val xml = JUnitXmlReport.render(Fixtures.result())
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(xml.byteInputStream())
        val root = doc.documentElement
        assertEquals("testsuites", root.tagName)
        assertEquals("4", root.getAttribute("tests"))
        assertEquals("Pi-Dash <UI> & tests", root.getAttribute("name"))
        assertEquals(2, doc.getElementsByTagName("testsuite").length)
        assertEquals(1, doc.getElementsByTagName("failure").length)
        assertEquals(1, doc.getElementsByTagName("error").length)
        assertEquals(1, doc.getElementsByTagName("skipped").length)
        assertTrue(xml.contains("[[ATTACHMENT|artifacts/a/video.mp4]]"))
        assertTrue(xml.contains("FLAKY: passed on attempt 2"))
    }

    @Test
    fun html_embedsDataSafely() {
        val dir = Files.createTempDirectory("atr-html").toFile()
        File(dir, "artifacts/x").mkdirs()
        File(dir, "artifacts/x/logcat.txt").writeText(
            "10-31 11:00:00.000  100  100 I TestRunner: started\n10-31 11:00:00.001  100  100 D Noise: hidden\n10-31 11:00:00.002  100  100 E Crash: </script>boom",
        )
        HtmlReport.write(Fixtures.result(), dir)
        val html = File(dir, "report.html").readText()
        assertFalse(html.contains("{{DATA}}") || html.contains("/*{{JS}}*/") || html.contains("/*{{CSS}}*/"))
        assertTrue(html.contains("Pi-Dash &lt;UI&gt; &amp; tests — ATR report"))
        assertFalse(html.contains("</script>boom"), "closing script tags inside data must be escaped")
        assertTrue(html.contains("TestRunner: started"))
        assertFalse(html.contains("Noise: hidden"), "logcat excerpt keeps only relevant lines")
    }
}

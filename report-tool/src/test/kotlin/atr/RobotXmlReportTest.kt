package atr

import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RobotXmlReportTest {
    private val outDir = File("build/tmp/robot-test/atr-results").absoluteFile
    private val xmlDir = File("build/tmp/robot-test/robot").absoluteFile

    private fun parse(xml: String): Element =
        DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(xml.byteInputStream()).documentElement

    private fun Element.children(tag: String) =
        (0 until childNodes.length).map { childNodes.item(it) }.filterIsInstance<Element>().filter { it.tagName == tag }

    private fun result() = Fixtures.result().let { r ->
        r.copy(tests = r.tests.mapIndexed { i, t -> if (i == 0) t.copy(tags = listOf("Smoke")) else t })
    }

    @Test
    fun schema5_mapsClassesToSuitesAndMethodsToTests() {
        val root = parse(RobotXmlReport.render(result(), outDir, xmlDir))
        assertEquals("5", root.getAttribute("schemaversion"))
        val top = root.children("suite").single()
        assertEquals("Pi-Dash <UI> & tests", top.getAttribute("name"))
        val suites = top.children("suite")
        assertEquals(listOf("DashboardScreenTest", "MediaCompanionTest"), suites.map { it.getAttribute("name") })
        assertEquals(listOf("muteAndTheme_toggled", "speedometer_overLimit"), suites[0].children("test").map { it.getAttribute("name") })
        assertEquals("FAIL", top.children("status").single().getAttribute("status"))
    }

    @Test
    fun flakyTest_passesWithFlakyTagAndOneKeywordPerAttempt() {
        val test = parse(RobotXmlReport.render(result(), outDir, xmlDir)).getElementsByTagName("test").item(0) as Element
        assertEquals(listOf("Smoke", "flaky"), test.children("tag").map { it.textContent })
        val keywords = test.children("kw")
        assertEquals(listOf("FAIL", "PASS"), keywords.map { it.children("status").single().getAttribute("status") })
        assertEquals("ATR", keywords[0].getAttribute("owner"))
        val status = test.children("status").single()
        assertEquals("PASS", status.getAttribute("status"))
        assertTrue(status.textContent.contains("flaky"))
        assertTrue(status.hasAttribute("start") && status.hasAttribute("elapsed"))
    }

    @Test
    fun errorBecomesFail_andSkipStaysSkip() {
        val tests = parse(RobotXmlReport.render(result(), outDir, xmlDir)).getElementsByTagName("test")
        val error = tests.item(2) as Element
        val skip = tests.item(3) as Element
        assertEquals("FAIL", error.children("status").single().getAttribute("status"))
        assertEquals("ERROR: Process crashed.", error.children("status").single().textContent)
        assertEquals("SKIP", skip.children("status").single().getAttribute("status"))
    }

    @Test
    fun artifactLinks_areRelativeToTheXmlFolder() {
        val xml = RobotXmlReport.render(result(), outDir, xmlDir)
        assertTrue(xml.contains("&lt;video src=\"../atr-results/artifacts/a/video.mp4#t=0.1\""), "video embedded with relative path")
        assertTrue(xml.contains("[../atr-results/report.html|report.html]"))
    }

    @Test
    fun legacySchema4_usesRobot6Attributes() {
        val root = parse(RobotXmlReport.render(result(), outDir, xmlDir, legacy = true))
        assertEquals("4", root.getAttribute("schemaversion"))
        val kw = root.getElementsByTagName("kw").item(0) as Element
        assertEquals("ATR", kw.getAttribute("library"))
        val status = kw.children("status").single()
        assertTrue(status.hasAttribute("starttime") && status.hasAttribute("endtime"))
        assertTrue((root.getElementsByTagName("msg").item(0) as Element).hasAttribute("timestamp"))
    }
}

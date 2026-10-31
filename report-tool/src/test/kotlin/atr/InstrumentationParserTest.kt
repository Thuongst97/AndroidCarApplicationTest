package atr

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class InstrumentationParserTest {

    private val test = TestId("com.app.FooTest", "bar")

    private fun block(code: Int, vararg extra: String) = buildString {
        appendLine("INSTRUMENTATION_STATUS: class=com.app.FooTest")
        appendLine("INSTRUMENTATION_STATUS: current=1")
        appendLine("INSTRUMENTATION_STATUS: id=AndroidJUnitRunner")
        appendLine("INSTRUMENTATION_STATUS: numtests=1")
        extra.forEach { appendLine(it) }
        appendLine("INSTRUMENTATION_STATUS: test=bar")
        appendLine("INSTRUMENTATION_STATUS_CODE: $code")
    }

    @Test
    fun passingTest_isPass() {
        val out = block(1, "INSTRUMENTATION_STATUS: stream=") + block(0, "INSTRUMENTATION_STATUS: stream=.") +
            "INSTRUMENTATION_RESULT: stream=\n\nOK (1 test)\n\nINSTRUMENTATION_CODE: -1\n"
        val parsed = InstrumentationParser.parse(out)
        val outcome = InstrumentationParser.outcomeOf(test, parsed, timedOut = false)
        assertEquals(Status.PASS, outcome.status)
        assertNull(outcome.message)
        assertEquals(-1, parsed.code)
    }

    @Test
    fun failure_keepsMultiLineStackTrace() {
        val stack = listOf(
            "INSTRUMENTATION_STATUS: stack=java.lang.AssertionError: Expected <5> but was <3>",
            "\tat org.junit.Assert.fail(Assert.java:89)",
            "\tat com.app.FooTest.bar(FooTest.kt:42)",
        ).toTypedArray()
        val out = block(1) + block(-2, *stack) + "INSTRUMENTATION_CODE: -1\n"
        val outcome = InstrumentationParser.outcomeOf(test, InstrumentationParser.parse(out), false)
        assertEquals(Status.FAIL, outcome.status)
        assertEquals("java.lang.AssertionError: Expected <5> but was <3>", outcome.message)
        assertTrue(outcome.stackTrace!!.contains("FooTest.kt:42"))
        assertEquals(3, outcome.stackTrace!!.lines().size)
    }

    @Test
    fun ignoredAndAssumption_areSkip() {
        assertEquals(Status.SKIP, InstrumentationParser.outcomeOf(test, InstrumentationParser.parse(block(1) + block(-3)), false).status)
        assertEquals(Status.SKIP, InstrumentationParser.outcomeOf(test, InstrumentationParser.parse(block(1) + block(-4)), false).status)
    }

    @Test
    fun processCrash_isErrorWithMessage() {
        val out = block(1) + "INSTRUMENTATION_RESULT: shortMsg=Process crashed.\nINSTRUMENTATION_CODE: 0\n"
        val outcome = InstrumentationParser.outcomeOf(test, InstrumentationParser.parse(out), false)
        assertEquals(Status.ERROR, outcome.status)
        assertEquals("Process crashed.", outcome.message)
    }

    @Test
    fun notFound_andTimeout_areErrors() {
        val notFound = InstrumentationParser.outcomeOf(test, InstrumentationParser.parse("INSTRUMENTATION_CODE: -1\n"), false)
        assertEquals(Status.ERROR, notFound.status)
        assertTrue(notFound.message!!.contains("not found"))
        assertEquals(Status.ERROR, InstrumentationParser.outcomeOf(test, InstrumentationParser.parse(""), timedOut = true).status)
    }

    @Test
    fun instrumentationFailed_isError() {
        val out = "INSTRUMENTATION_FAILED: com.app.test/androidx.test.runner.AndroidJUnitRunner\nINSTRUMENTATION_CODE: 0\n"
        val outcome = InstrumentationParser.outcomeOf(test, InstrumentationParser.parse(out), false)
        assertEquals(Status.ERROR, outcome.status)
        assertNotNull(outcome.message)
    }

    @Test
    fun listRun_returnsTestsInOrderWithoutDuplicates() {
        val out = """
            INSTRUMENTATION_STATUS: class=com.app.NavigationTest
            INSTRUMENTATION_STATUS: stream=
            com.app.NavigationTest:
            INSTRUMENTATION_STATUS: test=b
            INSTRUMENTATION_STATUS_CODE: 1
            INSTRUMENTATION_STATUS: class=com.app.NavigationTest
            INSTRUMENTATION_STATUS: stream=.
            INSTRUMENTATION_STATUS: test=b
            INSTRUMENTATION_STATUS_CODE: 0
            INSTRUMENTATION_STATUS: class=com.app.NavigationTest
            INSTRUMENTATION_STATUS: test=a
            INSTRUMENTATION_STATUS_CODE: 1
            INSTRUMENTATION_STATUS: class=com.app.NavigationTest
            INSTRUMENTATION_STATUS: test=a
            INSTRUMENTATION_STATUS_CODE: 0
            INSTRUMENTATION_CODE: -1
        """.trimIndent()
        val tests = InstrumentationParser.listedTests(InstrumentationParser.parse(out))
        assertEquals(listOf(TestId("com.app.NavigationTest", "b"), TestId("com.app.NavigationTest", "a")), tests)
    }

    @Test
    fun windowsLineEndings_areHandled() {
        val out = (block(1) + block(0)).replace("\n", "\r\n")
        assertEquals(Status.PASS, InstrumentationParser.outcomeOf(test, InstrumentationParser.parse(out), false).status)
    }
}

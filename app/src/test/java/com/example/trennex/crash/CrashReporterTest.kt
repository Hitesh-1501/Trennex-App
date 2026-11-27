package com.example.trennex.crash

import org.junit.Assert.*
import org.junit.Test
import java.io.IOException
import java.util.concurrent.CancellationException

class CrashReporterTest {
    @Test fun collectionIsRestrictedToRealStagingProject() {
        assertTrue(CrashEnvironment.canCollect("staging", "trennex-staging-ebbeb", false))
        assertFalse(CrashEnvironment.canCollect("staging", "trennex-staging-ebbeb", true))
        assertFalse(CrashEnvironment.canCollect("staging", "demo-trennex", false))
        assertFalse(CrashEnvironment.canCollect("staging", "trennex-ebbeb", false))
        assertFalse(CrashEnvironment.canCollect("debug", "trennex-staging-ebbeb", false))
        assertFalse(CrashEnvironment.canCollect("release", "trennex-ebbeb", false))
    }

    @Test fun handledReportDropsMessageCauseAndExternalFrames() {
        val source = IllegalStateException("OTP 123456 at private@example.com",
            IllegalStateException("private address"))
        source.stackTrace = arrayOf(
            StackTraceElement("com.example.trennex.viewmodel.home.HomeViewModel", "load", "HomeViewModel.kt", 42),
            StackTraceElement("com.thirdparty.Unsafe", "send", "Unsafe.kt", 7),
        )
        val safe = CrashPrivacy.sanitized(CrashArea.HOME, source)
        assertEquals("Unexpected home failure", safe.message)
        assertNull(safe.cause)
        assertEquals(1, safe.stackTrace.size)
        assertFalse(safe.toString().contains("123456"))
        assertFalse(safe.toString().contains("private"))
    }

    @Test fun expectedFailuresAndUnrestrictedLogsAreIgnored() {
        assertFalse(CrashPrivacy.shouldReport(CancellationException()))
        assertFalse(CrashPrivacy.shouldReport(IOException("offline")))
        assertFalse(CrashPrivacy.shouldReport(IllegalArgumentException("invalid input")))
        assertTrue(CrashPrivacy.shouldReport(IllegalStateException("unexpected")))
        assertTrue(CrashPrivacy.safeStep("retry"))
        assertFalse(CrashPrivacy.safeStep("query=private@example.com"))
    }

    @Test fun limiterDeduplicatesRepeatedErrorsAndCapsSession() {
        var now = 100_000L
        val gate = CrashReportGate { now }
        assertTrue(gate.allow(CrashArea.SEARCH))
        assertFalse(gate.allow(CrashArea.SEARCH))
        now += 60_000L
        assertTrue(gate.allow(CrashArea.SEARCH))
        for (area in CrashArea.entries.filter { it != CrashArea.SEARCH }.take(6)) {
            assertTrue(gate.allow(area))
        }
        assertFalse(gate.allow(CrashArea.STAGING_TEST))
    }

    @Test fun noOpNeverCollects() {
        val reporter = NoOpCrashReporter()
        reporter.setCollectionEnabled(true)
        reporter.log(CrashArea.HOME, "start")
        reporter.recordUnexpected(CrashArea.HOME, IllegalStateException("private"))
        assertFalse(reporter.isAvailable)
        assertFalse(reporter.isCollectionEnabled)
    }
}

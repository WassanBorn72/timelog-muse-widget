package app.timelog.musewidget

import org.junit.Assert.*
import org.junit.Test

class DestinationTest {
    @Test fun freshInstallDefaultsToEventRepository() {
        assertEquals("WassanBorn72/timelog-events", Destination.configured(null))
    }
    @Test fun legacyCodeSettingIsReplacedCaseInsensitively() {
        assertEquals(Destination.DEFAULT, Destination.configured("wassanborn72/TIMELOG-MUSE-WIDGET"))
        assertFalse(Destination.valid(Destination.CODE))
    }
    @Test fun customEventDestinationIsPreserved() {
        assertEquals("someone/private-events", Destination.configured("someone/private-events"))
        assertTrue(Destination.valid("someone/private-events"))
    }
    @Test fun transportRefusesCodeDestinationBeforeConnecting() {
        var connected = false
        val client = IssueClient { connected = true; throw AssertionError("Must not connect") }
        try {
            client.send(Event("id", "arrival", "2026-11-17T10:00:00+09:00", "test", Destination.CODE), "dummy")
            fail("Expected refusal")
        } catch (_: IllegalArgumentException) { assertFalse(connected) }
    }
}

package app.timelog.musewidget

import org.junit.Assert.*
import org.junit.Test

class SettingsTest {
    @Test fun repositoryMustBeAnOwnerAndName() {
        assertTrue(Settings.validRepository("WassanBorn72/timelog-events"))
        listOf("WassanBorn72/timelog-muse-widget", "wassanborn72/TIMELOG-MUSE-WIDGET", "", "repo", "https://github.com/owner/repo", "owner/repo/issues", "owner/repo?x", "owner/repo\n").forEach {
            assertFalse(it, Settings.validRepository(it))
        }
    }
}

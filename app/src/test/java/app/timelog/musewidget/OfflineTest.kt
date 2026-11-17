package app.timelog.musewidget

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.testing.TestListenableWorkerBuilder
import androidx.work.ListenableWorker
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [26, 28, 29])
class OfflineTest {
    private lateinit var context: Context
    @Before fun prepare() {
        context = ApplicationProvider.getApplicationContext()
        context.deleteDatabase("events.db")
        context.getSharedPreferences("settings", Context.MODE_PRIVATE).edit().clear().commit()
    }
    @Test fun missingTokenKeepsEveryTapAcrossDatabaseReopen() {
        val events = Event.types.map { Event.now(it, "owner/original") }
        Store(context).use { store -> events.forEach(store::add) }
        val worker = TestListenableWorkerBuilder<SyncWorker>(context).build()
        assertEquals(ListenableWorker.Result.success(), worker.doWork())
        assertTrue(Settings(context).status.contains("トークン未設定"))
        Store(context).use { store ->
            assertEquals(5, store.count())
            events.forEach { assertEquals(it, store.first()); store.remove(it.id) }
            assertNull(store.first())
        }
    }
    @Test fun legacyCodeQueueMovesWithoutChangingEventIdentityOrOtherDestinations() {
        val legacy = Event.now("arrival", "wassanborn72/TIMELOG-MUSE-WIDGET")
        val custom = Event.now("departure", "someone/private-events")
        Store(context).use { it.add(legacy); it.add(custom); it.retargetCodeQueue(Destination.DEFAULT) }
        Store(context).use {
            assertEquals(2, it.count())
            assertEquals(legacy.copy(repository = Destination.DEFAULT), it.first())
            it.remove(legacy.id)
            assertEquals(custom, it.first())
        }
    }
    @Test fun destinationChangeDoesNotRedirectPendingEvents() {
        val first = Event.now("departure", "owner/original")
        Store(context).use { it.add(first) }
        Settings(context).repository = "owner/new"
        Store(context).use { assertEquals("owner/original", it.first()!!.repository) }
    }
}

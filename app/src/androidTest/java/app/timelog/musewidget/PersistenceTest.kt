package app.timelog.musewidget

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.time.OffsetDateTime

@RunWith(AndroidJUnit4::class)
class PersistenceTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    @Test fun queueSurvivesReopenAndPreservesTapOrder() {
        context.deleteDatabase("events.db")
        val first = Event.now("sleep_start", "owner/original")
        val second = Event.now("sleep_start", "owner/new")
        assertNotEquals(first.id, second.id)
        OffsetDateTime.parse(first.timestamp)
        Store(context).use { it.add(first); it.add(second) }
        Store(context).use {
            assertEquals(2, it.count())
            assertEquals(first, it.first())
            it.remove(first.id)
            assertEquals(second, it.first())
        }
        Store(context).use { assertEquals(1, it.count()); it.remove(second.id); assertNull(it.first()) }
    }
    @Test fun widgetBroadcastsRecordAllFiveButtonsWithoutToken() {
        context.deleteDatabase("events.db")
        Settings(context).saveToken("")
        Event.types.forEach { type ->
            context.sendBroadcast(android.content.Intent(context, LifeWidget::class.java)
                .setAction("app.timelog.musewidget.TAP").putExtra("type", type))
        }
        val deadline = System.currentTimeMillis() + 10000
        while (Store(context).use { it.count() } < 5 && System.currentTimeMillis() < deadline) Thread.sleep(50)
        Store(context).use { assertEquals(5, it.count()) }
        assertTrue(Settings(context).status.contains("トークン未設定"))
    }
    @Test fun tokenIsEncryptedAndCanBeRemoved() {
        val settings = Settings(context)
        settings.saveToken("test-token-not-a-real-credential")
        assertEquals("test-token-not-a-real-credential", Settings(context).token())
        val stored = context.getSharedPreferences("settings", 0).getString("token", "")!!
        assertFalse(stored.contains("test-token"))
        settings.saveToken("")
        assertEquals("", settings.token())
    }
    @Test fun jsonContainsMuseFieldsForAllButtons() {
        Event.types.forEach { type ->
            val event = Event.now(type, "owner/repo")
            val json = event.json()
            assertEquals(type, json.getString("type"))
            assertEquals(event.timestamp, json.getString("timestamp"))
            assertEquals(event.id, json.getString("id"))
            assertTrue(json.has("device"))
        }
    }
}

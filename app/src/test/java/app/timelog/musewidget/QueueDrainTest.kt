package app.timelog.musewidget

import org.junit.Assert.*
import org.junit.Test
import java.io.IOException

class QueueDrainTest {
    @Test fun offlineFailureRetainsAllEventsThenRecoverySendsInOrder() {
        val disk = mutableListOf("sleep-1", "sleep-2", "arrival")
        val received = mutableListOf<String>()
        fun drain(online: Boolean) = QueueDrain(
            first = { disk.firstOrNull() },
            send = { event: String -> if (online) { received.add(event); true } else false },
            remove = { event: String -> disk.remove(event); Unit }, stopped = { false }
        ).run()
        assertFalse(drain(false))
        assertEquals(listOf("sleep-1", "sleep-2", "arrival"), disk)
        assertTrue(drain(true))
        assertEquals(listOf("sleep-1", "sleep-2", "arrival"), received)
        assertTrue(disk.isEmpty())
    }
    @Test fun rejectedHeadBlocksLaterEvents() {
        val queue = mutableListOf(1, 2, 3)
        val attempted = mutableListOf<Int>()
        val complete = QueueDrain({ queue.firstOrNull() }, { attempted.add(it); it != 2 }, { queue.remove(it); Unit }, { false }).run()
        assertFalse(complete)
        assertEquals(listOf(1, 2), attempted)
        assertEquals(listOf(2, 3), queue)
    }
    @Test fun stoppedWorkerLeavesQueueIntact() {
        val queue = mutableListOf(1)
        assertFalse(QueueDrain({ queue.firstOrNull() }, { fail("Must not send"); true }, { queue.remove(it); Unit }, { true }).run())
        assertEquals(listOf(1), queue)
    }
    @Test fun ambiguousNetworkExceptionCannotDeleteEvent() {
        val queue = mutableListOf(1)
        try {
            QueueDrain({ queue.firstOrNull() }, { throw IOException("response lost") }, { queue.remove(it); Unit }, { false }).run()
            fail("Expected network failure")
        } catch (_: IOException) { assertEquals(listOf(1), queue) }
    }
}

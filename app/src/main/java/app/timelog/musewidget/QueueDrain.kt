package app.timelog.musewidget

/** Removes only acknowledged events; a failure blocks later events to preserve FIFO. */
internal class QueueDrain<T>(
    private val first: () -> T?,
    private val send: (T) -> Boolean,
    private val remove: (T) -> Unit,
    private val stopped: () -> Boolean
) {
    fun run(): Boolean {
        while (!stopped()) {
            val event = first() ?: return true
            if (!send(event)) return false
            remove(event)
        }
        return false
    }
}

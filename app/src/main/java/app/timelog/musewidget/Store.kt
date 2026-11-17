package app.timelog.musewidget

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.os.Build
import org.json.JSONObject
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

data class Event(val id: String, val type: String, val timestamp: String, val device: String, val repository: String) {
    fun json() = JSONObject().put("id", id).put("type", type).put("timestamp", timestamp).put("device", device)
    companion object {
        val types = listOf("sleep_start", "sleep_end", "departure", "arrival", "concerta")
        fun now(type: String, repository: String): Event {
            require(type in types)
            return Event(UUID.randomUUID().toString(), type, OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME), Build.MODEL, repository)
        }
    }
}

// SQLiteOpenHelper only implements AutoCloseable on newer Android releases.
// Declare Closeable ourselves so Kotlin use() is safe on every supported API.
class Store(context: Context) : SQLiteOpenHelper(context, "events.db", null, 1), java.io.Closeable {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE events (sequence INTEGER PRIMARY KEY AUTOINCREMENT, id TEXT UNIQUE NOT NULL, type TEXT NOT NULL, timestamp TEXT NOT NULL, device TEXT NOT NULL, repository TEXT NOT NULL)")
    }
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
    fun add(event: Event) {
        writableDatabase.insertOrThrow("events", null, ContentValues().apply {
            put("id", event.id); put("type", event.type); put("timestamp", event.timestamp)
            put("device", event.device); put("repository", event.repository)
        })
    }
    fun first(): Event? = readableDatabase.rawQuery("SELECT id,type,timestamp,device,repository FROM events ORDER BY sequence LIMIT 1", null).use {
        if (it.moveToFirst()) Event(it.getString(0), it.getString(1), it.getString(2), it.getString(3), it.getString(4)) else null
    }
    // Only the formerly unsafe code destination is migrated; other destinations stay pinned.
    fun retargetCodeQueue(destination: String) {
        require(Destination.valid(destination))
        writableDatabase.update("events", ContentValues().apply { put("repository", destination) },
            "repository = ? COLLATE NOCASE", arrayOf(Destination.CODE))
    }
    fun count(): Int = readableDatabase.rawQuery("SELECT COUNT(*) FROM events", null).use { it.moveToFirst(); it.getInt(0) }
    fun remove(id: String) { writableDatabase.delete("events", "id=?", arrayOf(id)) }
}

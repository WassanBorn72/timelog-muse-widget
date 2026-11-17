package app.timelog.musewidget

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

internal class IssueClient(private val endpoint: (Event) -> URL = { URL("https://api.github.com/repos/${it.repository}/issues") }) {
    fun send(event: Event, token: String): Int {
        require(Destination.valid(event.repository)) { "Code repository cannot receive events" }
        val connection = endpoint(event).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "POST"
            connection.instanceFollowRedirects = false
            connection.connectTimeout = 15000
            connection.readTimeout = 20000
            connection.setRequestProperty("Authorization", "Bearer $token")
            connection.setRequestProperty("Accept", "application/vnd.github+json")
            connection.setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
            connection.setRequestProperty("User-Agent", "TimelogMuseWidget/1.0")
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            connection.doOutput = true
            val body = JSONObject().put("title", "${event.type} ${event.timestamp}")
                .put("body", "```json\n${event.json().toString(2)}\n```")
            connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            return connection.responseCode
        } finally { connection.disconnect() }
    }
}

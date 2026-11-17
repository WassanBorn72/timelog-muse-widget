package app.timelog.musewidget

import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class IssueClientTest {
    @Test fun createsIssueWithMuseJsonAndScopedAuth() {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setResponseCode(201).setBody("{}"))
            val event = Event("stable-id", "concerta", "2026-11-17T23:42:00+09:00", "test-device", "owner/repo")
            val client = IssueClient { server.url("/repos/${it.repository}/issues").toUrl() }
            assertEquals(201, client.send(event, "dummy-token"))
            val request = server.takeRequest()
            assertEquals("POST", request.method)
            assertEquals("/repos/owner/repo/issues", request.path)
            assertEquals("Bearer dummy-token", request.getHeader("Authorization"))
            val body = JSONObject(request.body.readUtf8())
            assertEquals("concerta 2026-11-17T23:42:00+09:00", body.getString("title"))
            val json = JSONObject(body.getString("body").removePrefix("```json\n").removeSuffix("\n```"))
            assertEquals("concerta", json.getString("type"))
            assertEquals("test-device", json.getString("device"))
            assertEquals("stable-id", json.getString("id"))
            assertEquals(event.timestamp, json.getString("timestamp"))
        }
    }
    @Test fun authorizationFailureIsReturnedWithoutSuccess() {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setResponseCode(401))
            assertEquals(401, IssueClient { server.url("/issues").toUrl() }.send(Event.now("arrival", "owner/repo"), "invalid"))
        }
    }
    @Test fun redirectIsNotFollowedWithCredential() {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setResponseCode(302).setHeader("Location", server.url("/other")))
            assertEquals(302, IssueClient { server.url("/issues").toUrl() }.send(Event.now("arrival", "owner/repo"), "dummy"))
            assertEquals(1, server.requestCount)
        }
    }
}

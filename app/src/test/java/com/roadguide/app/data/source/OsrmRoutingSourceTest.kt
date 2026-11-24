package com.roadguide.app.data.source

import com.roadguide.app.domain.model.RouteProfile
import com.roadguide.app.map.model.MapPoint
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class OsrmRoutingSourceTest {

    @Test
    fun routeUsesConfiguredBasePathAndSeparateOsrmPathSegments() = runBlocking {
        val server = MockWebServer()
        server.enqueue(
            MockResponse().setBody(
                """
                {
                  "code": "Ok",
                  "routes": [{
                    "distance": 1000,
                    "duration": 120,
                    "geometry": {
                      "type": "LineString",
                      "coordinates": [[37.0, 55.0], [38.0, 56.0]]
                    }
                  }]
                }
                """.trimIndent()
            )
        )
        server.start()

        try {
            val source = OsrmRoutingSource(
                okHttpClient = OkHttpClient(),
                baseUrl = server.url("/custom/base/").toString(),
                maxRetries = 1
            )

            val routes = source.route(
                origin = MapPoint(latitude = 55.0, longitude = 37.0),
                destination = MapPoint(latitude = 56.0, longitude = 38.0),
                profile = RouteProfile.WALKING
            )

            assertNotNull(routes)
            assertEquals(
                listOf("custom", "base", "route", "v1", "walking", "37.0,55.0;38.0,56.0"),
                server.takeRequest().requestUrl?.pathSegments
            )
        } finally {
            server.shutdown()
        }
    }
}

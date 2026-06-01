package com.fenlight.companion.data.api

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

/**
 * Verifies that the Trakt list-items endpoint surfaces the paging total via the
 * X-Pagination-Page-Count response header (Trakt returns paging in headers, not the body).
 */
class TraktApiPaginationTest {

    private lateinit var server: MockWebServer
    private lateinit var api: TraktApi

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
        api = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(TraktApi::class.java)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun myListItems_parsesPageCountHeaderAndEmptyBody() = runBlocking {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("X-Pagination-Page-Count", "7")
                .setBody("[]"),
        )

        val response = api.myListItems(slug = "my-list", page = 1)

        val totalPages = response.headers()["X-Pagination-Page-Count"]?.toIntOrNull() ?: 1
        assertEquals(7, totalPages)
        assertTrue(response.body().isNullOrEmpty())
    }
}

/*
 * Copyright (c) 2021 DuckDuckGo
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.duckduckgo.app.trackerdetection

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.duckduckgo.app.CoroutineTestRule
import com.duckduckgo.app.privacy.db.UserWhitelistDao
import com.duckduckgo.app.runBlocking
import com.duckduckgo.app.trackerdetection.Client.ClientName
import com.duckduckgo.app.trackerdetection.Client.ClientName.EASYLIST
import com.duckduckgo.app.trackerdetection.Client.ClientName.EASYPRIVACY
import com.duckduckgo.app.trackerdetection.model.TrackingEvent
import com.duckduckgo.privacy.config.api.ContentBlocking
import com.duckduckgo.privacy.config.api.TrackerAllowlist
import com.nhaarman.mockitokotlin2.anyOrNull
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyString

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
class TrackerDetectorTest {

    @get:Rule
    @Suppress("unused")
    val coroutineRule = CoroutineTestRule()

    private val mockEntityLookup: EntityLookup = mock()
    private val mockUserWhitelistDao: UserWhitelistDao = mock()
    private val mockContentBlocking: ContentBlocking = mock()
    private val mockTrackerAllowlist: TrackerAllowlist = mock()
    private val trackerDetector = TrackerDetectorImpl(mockEntityLookup, mockUserWhitelistDao, mockContentBlocking, mockTrackerAllowlist)

    @Before
    fun setupTest() {
        coroutineRule.runBlocking {
            whenever(mockContentBlocking.isAnException(anyOrNull())).thenReturn(false)
            whenever(mockTrackerAllowlist.isAnException(anyOrNull(), anyOrNull())).thenReturn(false)
        }
    }

    @Test
    fun whenThereAreNoClientsThenClientCountIsZero() {
        assertEquals(0, trackerDetector.clientCount)
    }

    @Test
    fun whenClientAddedThenClientCountIsOne() {
        trackerDetector.addClient(alwaysMatchingClient(CLIENT_A))
        assertEquals(1, trackerDetector.clientCount)
    }

    @Test
    fun whenTwoClientsWithDifferentNamesAddedThenCountIsTwo() {
        trackerDetector.addClient(alwaysMatchingClient(CLIENT_A))
        trackerDetector.addClient(alwaysMatchingClient(CLIENT_B))
        assertEquals(2, trackerDetector.clientCount)
    }

    @Test
    fun whenTwoClientsWithSameNameAddedThenClientIsReplacedAndCountIsStillOne() = coroutineRule.runBlocking {
        trackerDetector.addClient(alwaysMatchingClient(CLIENT_A))
        assertEquals(1, trackerDetector.clientCount)
        assertNotNull(trackerDetector.evaluate("http://thirdparty.com/update.js", "http://example.com/index.com"))

        trackerDetector.addClient(neverMatchingClient(CLIENT_A))
        assertEquals(1, trackerDetector.clientCount)
        assertNull(trackerDetector.evaluate("http://thirdparty.com/update.js", "http://example.com/index.com"))
    }

    @Test
    fun whenThereAreNoClientsThenEvaluateReturnsNull() = coroutineRule.runBlocking {
        assertNull(trackerDetector.evaluate("http://thirdparty.com/update.js", "http://example.com/index.com"))
    }

    @Test
    fun whenAllClientsFailToMatchThenEvaluateReturnsNull() = coroutineRule.runBlocking {
        trackerDetector.addClient(neverMatchingClient(CLIENT_A))
        trackerDetector.addClient(neverMatchingClient(CLIENT_B))
        assertNull(trackerDetector.evaluate("http://thirdparty.com/update.js", "http://example.com/index.com"))
    }

    @Test
    fun whenSiteIsNotUserWhitelistedAndAllClientsMatchThenEvaluateReturnsBlockedTrackingEvent() = coroutineRule.runBlocking {
        whenever(mockUserWhitelistDao.contains("example.com")).thenReturn(false)
        trackerDetector.addClient(alwaysMatchingClient(CLIENT_A))
        trackerDetector.addClient(alwaysMatchingClient(CLIENT_B))
        val expected = TrackingEvent("http://example.com/index.com", "http://thirdparty.com/update.js", null, null, true, null)
        val actual = trackerDetector.evaluate("http://thirdparty.com/update.js", "http://example.com/index.com")
        assertEquals(expected, actual)
    }

    @Test
    fun whenSiteIsUserWhitelistedAndAllClientsMatchThenEvaluateReturnsUnblockedTrackingEvent() = coroutineRule.runBlocking {
        whenever(mockUserWhitelistDao.contains("example.com")).thenReturn(true)
        trackerDetector.addClient(alwaysMatchingClient(CLIENT_A))
        trackerDetector.addClient(alwaysMatchingClient(CLIENT_B))
        val expected = TrackingEvent("http://example.com/index.com", "http://thirdparty.com/update.js", null, null, false, null)
        val actual = trackerDetector.evaluate("http://thirdparty.com/update.js", "http://example.com/index.com")
        assertEquals(expected, actual)
    }

    @Test
    fun whenSiteIsNotUserWhitelistedAndSomeClientsMatchThenEvaluateReturnsBlockedTrackingEvent() = coroutineRule.runBlocking {
        whenever(mockUserWhitelistDao.contains("example.com")).thenReturn(false)
        trackerDetector.addClient(neverMatchingClient(CLIENT_A))
        trackerDetector.addClient(alwaysMatchingClient(CLIENT_B))
        val expected = TrackingEvent("http://example.com/index.com", "http://thirdparty.com/update.js", null, null, true, null)
        val actual = trackerDetector.evaluate("http://thirdparty.com/update.js", "http://example.com/index.com")
        assertEquals(expected, actual)
    }

    @Test
    fun whenSiteIsUserWhitelistedAndSomeClientsMatchThenEvaluateReturnsUnblockedTrackingEvent() = coroutineRule.runBlocking {
        whenever(mockUserWhitelistDao.contains("example.com")).thenReturn(true)
        trackerDetector.addClient(neverMatchingClient(CLIENT_A))
        trackerDetector.addClient(alwaysMatchingClient(CLIENT_B))
        val expected = TrackingEvent("http://example.com/index.com", "http://thirdparty.com/update.js", null, null, false, null)
        val actual = trackerDetector.evaluate("http://thirdparty.com/update.js", "http://example.com/index.com")
        assertEquals(expected, actual)
    }

    @Test
    fun whenSiteIsInContentBlockingExceptionsListAndSomeClientsMatchThenEvaluateReturnsUnblockedTrackingEvent() = coroutineRule.runBlocking {
        whenever(mockContentBlocking.isAnException(anyString())).thenReturn(true)
        trackerDetector.addClient(alwaysMatchingClient(CLIENT_A))
        val expected = TrackingEvent("http://example.com/index.com", "http://thirdparty.com/update.js", null, null, false, null)
        val actual = trackerDetector.evaluate("http://thirdparty.com/update.js", "http://example.com/index.com")
        assertEquals(expected, actual)
    }

    @Test
    fun whenSiteIsNotUserWhitelistedAndSomeClientsMatchWithSurrogateThenEvaluateReturnsBlockedTrackingEventWithSurrogate() =
        coroutineRule.runBlocking {
            whenever(mockUserWhitelistDao.contains("example.com")).thenReturn(false)
            trackerDetector.addClient(alwaysMatchingClientWithSurrogate(CLIENT_A))
            val expected = TrackingEvent("http://example.com/index.com", "http://thirdparty.com/update.js", null, null, true, "testId")
            val actual = trackerDetector.evaluate("http://thirdparty.com/update.js", "http://example.com/index.com")
            assertEquals(expected, actual)
        }

    @Test
    fun whenRequestIsInAllowlistAndSomeClientsMatchThenEvaluateReturnsUnblockedTrackingEvent() = coroutineRule.runBlocking {
        whenever(mockTrackerAllowlist.isAnException(anyString(), anyString())).thenReturn(true)
        trackerDetector.addClient(alwaysMatchingClient(CLIENT_A))
        val expected = TrackingEvent("http://example.com/index.com", "http://thirdparty.com/update.js", null, null, false, null)
        val actual = trackerDetector.evaluate("http://thirdparty.com/update.js", "http://example.com/index.com")
        assertEquals(expected, actual)
    }

    @Test
    fun whenUrlHasSameDomainAsDocumentThenEvaluateReturnsNull() = coroutineRule.runBlocking {
        trackerDetector.addClient(alwaysMatchingClient(CLIENT_A))
        assertNull(trackerDetector.evaluate("http://example.com/update.js", "http://example.com/index.com"))
    }

    @Test
    fun whenUrlIsSubdomainOfDocumentThenEvaluateReturnsNull() = coroutineRule.runBlocking {
        trackerDetector.addClient(alwaysMatchingClient(CLIENT_A))
        assertNull(trackerDetector.evaluate("http://mobile.example.com/update.js", "http://example.com/index.com"))
    }

    @Test
    fun whenUrlIsParentOfDocumentThenEvaluateReturnsNull() = coroutineRule.runBlocking {
        trackerDetector.addClient(alwaysMatchingClient(CLIENT_A))
        assertNull(trackerDetector.evaluate("http://example.com/update.js", "http://mobile.example.com/index.com"))
    }

    private fun alwaysMatchingClient(name: ClientName): Client {
        val client: Client = mock()
        whenever(client.name).thenReturn(name)
        whenever(client.matches(anyString(), anyString())).thenReturn(Client.Result(true))
        return client
    }

    private fun neverMatchingClient(name: ClientName): Client {
        val client: Client = mock()
        whenever(client.name).thenReturn(name)
        whenever(client.matches(anyString(), anyString())).thenReturn(Client.Result(false))
        return client
    }

    private fun alwaysMatchingClientWithSurrogate(name: ClientName): Client {
        val client: Client = mock()
        whenever(client.name).thenReturn(name)
        whenever(client.matches(anyString(), anyString())).thenReturn(Client.Result(matches = true, surrogate = "testId"))
        return client
    }

    companion object {
        // It doesn't matter what the value of these is they just need to be different
        private val CLIENT_A = EASYLIST
        private val CLIENT_B = EASYPRIVACY
    }
}

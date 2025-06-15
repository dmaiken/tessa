package io

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.ktor.server.routing.RoutingRequest
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class HttpUtilsTest {
    val request = mockk<RoutingRequest>()

    @Test
    fun `getEntryId returns entryId if valid long value`() {
        val entryId = 123L
        every {
            request.queryParameters["entryId"]
        } returns entryId.toString()

        getEntryId(request) shouldBe entryId
    }

    @Test
    fun `getEntryId returns null if no entryId is in request`() {
        every {
            request.queryParameters["entryId"]
        } returns null

        getEntryId(request) shouldBe null
    }

    @ParameterizedTest
    @ValueSource(strings = ["-1", "abc", "123.456"])
    fun `getEntryId throws if value is invalid`(entryId: String) {
        every {
            request.queryParameters["entryId"]
        } returns entryId

        shouldThrow<IllegalArgumentException> {
            getEntryId(request)
        }
    }
}

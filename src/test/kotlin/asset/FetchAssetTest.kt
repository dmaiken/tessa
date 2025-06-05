package io.asset

import io.BaseTest
import io.config.testWithTestcontainers
import io.kotest.matchers.shouldBe
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.util.createJsonClient
import org.junit.jupiter.api.Test
import java.util.UUID

class FetchAssetTest : BaseTest() {
    @Test
    fun `fetching an asset with an incorrect format returns bad request`() =
        testWithTestcontainers(postgres, localstack) {
            val client = createJsonClient()
            client.get("/assets/${UUID.randomUUID()}?format=invalid").apply {
                status shouldBe HttpStatusCode.BadRequest
            }
        }
}

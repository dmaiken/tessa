package io

import io.config.testWithTestcontainers
import io.image.AssetResponse
import io.image.StoreAssetRequest
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.*

class ImagePreProcessingTest : BaseTest() {

    @Test
    fun `image is resized when it is too large`() = testWithTestcontainers(
        postgres, localstack, mapOf(
            "image.preprocessing.enabled" to "true",
            "image.preprocessing.maxWidth" to "100",
        )
    ) {
        val client = createClient {
            install(ContentNegotiation) {
                json()
            }
        }
        val image = javaClass.getResourceAsStream("/images/img.png")!!.readBytes()
        val id = UUID.randomUUID()
        val request = StoreAssetRequest(
            id = id,
            fileName = "filename.jpeg",
            type = "image/png",
            alt = "an image",
            createdAt = LocalDateTime.now(),
        )
        var storeAssetResponse: AssetResponse? = null
        client.post("/assets") {
            contentType(ContentType.MultiPart.FormData)
            setBody(
                MultiPartFormDataContent(
                    formData {
                        append("metadata", Json.encodeToString<StoreAssetRequest>(request), Headers.build {
                            append(HttpHeaders.ContentType, "application/json")
                        })
                        append("file", image, Headers.build {
                            append(HttpHeaders.ContentType, "image/png")
                            append(HttpHeaders.ContentDisposition, "filename=\"ktor_logo.png\"")
                        })
                    },
                    BOUNDARY,
                    ContentType.MultiPart.FormData.withParameter("boundary", BOUNDARY)
                )
            )
        }.apply {
            status shouldBe HttpStatusCode.Created
            body<AssetResponse>().apply {
                this.id shouldBe id
                createdAt shouldNotBe null
                bucket shouldBe "assets"
                storeKey shouldNotBe null
                type shouldBe "image/png"
                alt shouldBe "an image"
                width shouldBe 100
            }.also {
                storeAssetResponse = it
            }
        }
    }
}
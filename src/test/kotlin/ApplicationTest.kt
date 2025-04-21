package io

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

class ApplicationTest : BaseTest() {

    @Test
    fun `can create and get image`() = testWithTestcontainers(postgres, localstack) {
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
            }.also {
                storeAssetResponse = it
            }
        }
        client.get("/assets/$id/info").apply {
            status shouldBe HttpStatusCode.OK
            body<AssetResponse>() shouldBe storeAssetResponse
        }
    }

    @Test
    fun `uploading something not an image will return bad request`() = testWithTestcontainers(postgres, localstack) {
        val client = createClient {
            install(ContentNegotiation) { json() }
        }
        val image = "I am not an image".toByteArray()
        val id = UUID.randomUUID()
        val request = StoreAssetRequest(
            id = id,
            fileName = "filename.jpeg",
            type = "image/png",
            alt = "an image",
            createdAt = LocalDateTime.now(),
        )
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
            status shouldBe HttpStatusCode.BadRequest
        }
    }

    @Test
    fun `fetching image info of image that does not exist returns not found`() =
        testWithTestcontainers(postgres, localstack) {
            val client = createClient {
                install(ContentNegotiation) { json() }
            }
            client.get("/assets/${UUID.randomUUID()}/info").apply {
                status shouldBe HttpStatusCode.NotFound
            }
        }

    @Test
    fun `fetching image that does not exist returns not found`() = testWithTestcontainers(postgres, localstack) {
        val client = createClient {
            install(ContentNegotiation) { json() }
        }
        client.get("/assets/${UUID.randomUUID()}").apply {
            status shouldBe HttpStatusCode.NotFound
        }
    }
}

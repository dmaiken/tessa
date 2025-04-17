package io

import io.image.ImageResponse
import io.image.StoreImageRequest
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
        val request = StoreImageRequest(
            id = id,
            fileName = "filename.jpeg",
            type = "image/png",
            alt = "an image",
            createdAt = LocalDateTime.now(),
        )
        var storeImageResponse: ImageResponse? = null
        client.post("/images") {
            contentType(ContentType.MultiPart.FormData)
            setBody(
                MultiPartFormDataContent(
                    formData {
                        append("metadata", Json.encodeToString<StoreImageRequest>(request), Headers.build {
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
            body<ImageResponse>().apply {
                this.id shouldBe id
                createdAt shouldNotBe null
                bucket shouldBe "images"
                storeKey shouldNotBe null
                type shouldBe "image/png"
                alt shouldBe "an image"
            }.also {
                storeImageResponse = it
            }
        }
        client.get("/images/$id/info").apply {
            status shouldBe HttpStatusCode.OK
            body<ImageResponse>() shouldBe storeImageResponse
        }
    }

    @Test
    fun `uploading something not an image will return bad request`() = testWithTestcontainers(postgres, localstack) {
        val client = createClient {
            install(ContentNegotiation) { json() }
        }
        val image = "I am not an image".toByteArray()
        val id = UUID.randomUUID()
        val request = StoreImageRequest(
            id = id,
            fileName = "filename.jpeg",
            type = "image/png",
            alt = "an image",
            createdAt = LocalDateTime.now(),
        )
        client.post("/images") {
            contentType(ContentType.MultiPart.FormData)
            setBody(
                MultiPartFormDataContent(
                    formData {
                        append("metadata", Json.encodeToString<StoreImageRequest>(request), Headers.build {
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
            client.get("/images/${UUID.randomUUID()}/info").apply {
                status shouldBe HttpStatusCode.NotFound
            }
        }

    @Test
    fun `fetching image that does not exist returns not found`() = testWithTestcontainers(postgres, localstack) {
        val client = createClient {
            install(ContentNegotiation) { json() }
        }
        client.get("/images/${UUID.randomUUID()}").apply {
            status shouldBe HttpStatusCode.NotFound
        }
    }
}

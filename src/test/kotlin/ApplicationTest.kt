package io

import asset.StoreAssetRequest
import io.config.testWithTestcontainers
import io.image.AssetResponse
import io.kotest.matchers.collections.shouldHaveSize
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
import java.util.*

class ApplicationTest : BaseTest() {

    @Test
    fun `can create and get image`() = testWithTestcontainers(postgres, localstack) {
        val client = createClient {
            install(ContentNegotiation) { json() }
        }
        val image = javaClass.getResourceAsStream("/images/img.png")!!.readBytes()
        val request = StoreAssetRequest(
            fileName = "filename.jpeg",
            type = "image/png",
            alt = "an image",
        )
        var storeAssetResponse: AssetResponse?
        client.post("/assets/profile") {
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
                id shouldNotBe null
                createdAt shouldNotBe null
                bucket shouldBe "assets"
                storeKey shouldNotBe null
                type shouldBe "image/png"
                alt shouldBe "an image"
            }.also {
                storeAssetResponse = it
            }
//            headers.get("location") shouldBe "http://localhost:8080/assets/profile"
        }
        client.get("/assets/profile/info").apply {
            status shouldBe HttpStatusCode.OK
            body<AssetResponse>() shouldBe storeAssetResponse
        }
    }

    @Test
    fun `creating asset on same path results in most recent being fetched`() =
        testWithTestcontainers(postgres, localstack) {
            val client = createClient {
                install(ContentNegotiation) { json() }
            }
            val image = javaClass.getResourceAsStream("/images/img.png")!!.readBytes()
            val request = StoreAssetRequest(
                fileName = "filename.jpeg",
                type = "image/png",
                alt = "an image",
            )
            val ids = mutableListOf<UUID>()
            repeat(2) {
                client.post("/assets/profile") {
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
                        id shouldNotBe null
                        ids.add(id)
                    }
                }
            }
            ids shouldHaveSize 2
            client.get("/assets/profile/info").apply {
                status shouldBe HttpStatusCode.OK
                body<AssetResponse>().apply {
                    ids[1] shouldBe id
                }
            }
        }

    @Test
    fun `uploading something not an image will return bad request`() = testWithTestcontainers(postgres, localstack) {
        val client = createClient {
            install(ContentNegotiation) { json() }
        }
        val image = "I am not an image".toByteArray()
        val request = StoreAssetRequest(
            fileName = "filename.jpeg",
            type = "image/png",
            alt = "an image",
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

package io.asset

import asset.StoreAssetRequest
import io.BaseTest
import io.config.testWithTestcontainers
import io.image.AssetResponse
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import io.util.createJsonClient
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import java.util.*

class FetchAssetTest : BaseTest() {

    @Test
    fun `getting all asset info with path returns all info`() =
        testWithTestcontainers(postgres, localstack) {
            val client = createJsonClient()
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

            client.get("/assets/profile/info/all").apply {
                status shouldBe HttpStatusCode.OK
                body<List<AssetResponse>>().apply {
                    size shouldBe 2
                    get(0).id shouldBe ids[1]
                    get(1).id shouldBe ids[0]
                }
            }
        }

    @Test
    fun `fetching image info of image that does not exist returns not found`() =
        testWithTestcontainers(postgres, localstack) {
            val client = createJsonClient()
            client.get("/assets/${UUID.randomUUID()}/info").apply {
                status shouldBe HttpStatusCode.NotFound
            }
        }

    @Test
    fun `fetching image that does not exist returns not found`() = testWithTestcontainers(postgres, localstack) {
        val client = createJsonClient()
        client.get("/assets/${UUID.randomUUID()}").apply {
            status shouldBe HttpStatusCode.NotFound
        }
    }
}
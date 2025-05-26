package io.asset

import asset.StoreAssetRequest
import io.BaseTest
import io.config.testWithTestcontainers
import io.image.AssetResponse
import io.kotest.matchers.shouldBe
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.util.createJsonClient
import io.util.storeAsset
import org.junit.jupiter.api.Test
import java.util.*

class DeleteAssetTest : BaseTest() {

    @Test
    fun `deleting asset that does not exist returns no content`() = testWithTestcontainers(postgres, localstack) {
        val client = createJsonClient()
        client.delete("/assets/${UUID.randomUUID()}")
            .apply {
                status shouldBe HttpStatusCode.NoContent
                bodyAsText() shouldBe ""
            }
    }

    @Test
    fun `can delete asset by path`() = testWithTestcontainers(postgres, localstack) {
        val client = createJsonClient()
        val image = javaClass.getResourceAsStream("/images/img.png")!!.readBytes()
        val request = StoreAssetRequest(
            fileName = "filename.png",
            type = "image/png",
            alt = "an image",
        )
        storeAsset(client, image, request, path = "profile")

        client.get("/assets/profile?format=metadata").apply {
            status shouldBe HttpStatusCode.OK
        }
        client.delete("/assets/profile").status shouldBe HttpStatusCode.NoContent
        client.get("/assets/profile?format=metadata").apply {
            status shouldBe HttpStatusCode.NotFound
        }
        client.delete("/assets/profile").status shouldBe HttpStatusCode.NoContent
    }

    @Test
    fun `deleting asset by path causes next oldest asset to be returned when fetching by path`() =
        testWithTestcontainers(postgres, localstack) {
            val client = createJsonClient()
            val image = javaClass.getResourceAsStream("/images/img.png")!!.readBytes()
            val request = StoreAssetRequest(
                fileName = "filename.png",
                type = "image/png",
                alt = "an image",
            )
            val firstAsset = storeAsset(client, image, request, path = "profile")
            val secondAsset = storeAsset(client, image, request, path = "profile")

            client.get("/assets/profile?format=metadata").apply {
                status shouldBe HttpStatusCode.OK
                body<AssetResponse>().apply {
                    id shouldBe secondAsset.id
                    entryId shouldBe secondAsset.entryId
                }
            }
            client.delete("/assets/profile").status shouldBe HttpStatusCode.NoContent
            client.get("/assets/profile?format=metadata").apply {
                status shouldBe HttpStatusCode.OK
                body<AssetResponse>().apply {
                    id shouldBe firstAsset.id
                    entryId shouldBe firstAsset.entryId
                }
            }
        }

    @Test
    fun `can delete asset by path and entryId`() = testWithTestcontainers(postgres, localstack) {
        val client = createJsonClient()
        val image = javaClass.getResourceAsStream("/images/img.png")!!.readBytes()
        val request = StoreAssetRequest(
            fileName = "filename.png",
            type = "image/png",
            alt = "an image",
        )
        val firstAsset = storeAsset(client, image, request, path = "profile")
        val secondAsset = storeAsset(client, image, request, path = "profile")

        client.delete("/assets/profile?entryId=${firstAsset.entryId}").status shouldBe HttpStatusCode.NoContent
        client.get("/assets/profile?format=metadata").apply {
            status shouldBe HttpStatusCode.OK
            body<AssetResponse>().apply {
                id shouldBe secondAsset.id
                entryId shouldBe secondAsset.entryId
            }
        }
        client.get("/assets/profile?format=metadata&entryId=${firstAsset.entryId}").apply {
            status shouldBe HttpStatusCode.NotFound
        }
    }

    @Test
    fun `cannot supply invalid entryId when deleting asset`() = testWithTestcontainers(postgres, localstack) {
        val client = createJsonClient()
        client.delete("/assets/profile?entryId=notANumber").status shouldBe HttpStatusCode.BadRequest
    }

    @Test
    fun `cannot supply negative entryId when deleting asset`() = testWithTestcontainers(postgres, localstack) {
        val client = createJsonClient()
        client.delete("/assets/profile?entryId=-1").status shouldBe HttpStatusCode.BadRequest
    }
}
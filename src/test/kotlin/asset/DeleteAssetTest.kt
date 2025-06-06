package io.asset

import asset.AssetResponse
import asset.StoreAssetRequest
import io.BaseTest
import io.config.testWithTestcontainers
import io.kotest.matchers.shouldBe
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.util.createJsonClient
import io.util.fetchAssetInfo
import io.util.storeAsset
import org.junit.jupiter.api.Test
import java.util.UUID

class DeleteAssetTest : BaseTest() {
    @Test
    fun `deleting asset that does not exist returns no content`() =
        testWithTestcontainers(postgres, localstack) {
            val client = createJsonClient()
            client.delete("/assets/${UUID.randomUUID()}")
                .apply {
                    status shouldBe HttpStatusCode.NoContent
                    bodyAsText() shouldBe ""
                }
        }

    @Test
    fun `can delete asset by path`() =
        testWithTestcontainers(postgres, localstack) {
            val client = createJsonClient()
            val image = javaClass.getResourceAsStream("/images/img.png")!!.readBytes()
            val request =
                StoreAssetRequest(
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
            val request =
                StoreAssetRequest(
                    fileName = "filename.png",
                    type = "image/png",
                    alt = "an image",
                )
            val firstAsset = storeAsset(client, image, request, path = "profile")
            val secondAsset = storeAsset(client, image, request, path = "profile")

            client.get("/assets/profile?format=metadata").apply {
                status shouldBe HttpStatusCode.OK
                body<AssetResponse>().apply {
                    id shouldBe secondAsset!!.id
                    entryId shouldBe secondAsset.entryId
                }
            }
            client.delete("/assets/profile").status shouldBe HttpStatusCode.NoContent
            client.get("/assets/profile?format=metadata").apply {
                status shouldBe HttpStatusCode.OK
                body<AssetResponse>().apply {
                    id shouldBe firstAsset!!.id
                    entryId shouldBe firstAsset.entryId
                }
            }
        }

    @Test
    fun `can delete asset by path and entryId`() =
        testWithTestcontainers(postgres, localstack) {
            val client = createJsonClient()
            val image = javaClass.getResourceAsStream("/images/img.png")!!.readBytes()
            val request =
                StoreAssetRequest(
                    fileName = "filename.png",
                    type = "image/png",
                    alt = "an image",
                )
            val firstAsset = storeAsset(client, image, request, path = "profile")
            val secondAsset = storeAsset(client, image, request, path = "profile")

            client.delete("/assets/profile?entryId=${firstAsset!!.entryId}").status shouldBe HttpStatusCode.NoContent
            client.get("/assets/profile?format=metadata").apply {
                status shouldBe HttpStatusCode.OK
                body<AssetResponse>().apply {
                    id shouldBe secondAsset!!.id
                    entryId shouldBe secondAsset.entryId
                }
            }
            client.get("/assets/profile?format=metadata&entryId=${firstAsset.entryId}").apply {
                status shouldBe HttpStatusCode.NotFound
            }
        }

    @Test
    fun `cannot supply invalid entryId when deleting asset`() =
        testWithTestcontainers(postgres, localstack) {
            val client = createJsonClient()
            client.delete("/assets/profile?entryId=notANumber").status shouldBe HttpStatusCode.BadRequest
        }

    @Test
    fun `cannot supply negative entryId when deleting asset`() =
        testWithTestcontainers(postgres, localstack) {
            val client = createJsonClient()
            client.delete("/assets/profile?entryId=-1").status shouldBe HttpStatusCode.BadRequest
        }

    @Test
    fun `can delete assets at path but not recursively`() =
        testWithTestcontainers(postgres, localstack) {
            val client = createJsonClient()
            val image = javaClass.getResourceAsStream("/images/img.png")!!.readBytes()
            val request =
                StoreAssetRequest(
                    fileName = "filename.png",
                    type = "image/png",
                    alt = "an image",
                )
            val firstAsset = storeAsset(client, image, request, path = "user/123")
            val secondAsset = storeAsset(client, image, request, path = "user/123")
            val assetToNotDelete = storeAsset(client, image, request, path = "user/123/profile")

            client.delete("/assets/user/123?mode=children").status shouldBe HttpStatusCode.NoContent

            fetchAssetInfo(client, "user/123", entryId = null, HttpStatusCode.NotFound)
            fetchAssetInfo(client, "user/123", firstAsset!!.entryId, HttpStatusCode.NotFound)
            fetchAssetInfo(client, "user/123", secondAsset!!.entryId, HttpStatusCode.NotFound)

            fetchAssetInfo(client, "user/123/profile", assetToNotDelete!!.entryId)
            fetchAssetInfo(client, "user/123/profile")
        }

    @Test
    fun `can delete assets at path recursively`() =
        testWithTestcontainers(postgres, localstack) {
            val client = createJsonClient()
            val image = javaClass.getResourceAsStream("/images/img.png")!!.readBytes()
            val request =
                StoreAssetRequest(
                    fileName = "filename.png",
                    type = "image/png",
                    alt = "an image",
                )
            val control = storeAsset(client, image, request, path = "user")
            val firstAsset = storeAsset(client, image, request, path = "user/123")
            val secondAsset = storeAsset(client, image, request, path = "user/123")
            val thirdAsset = storeAsset(client, image, request, path = "user/123/profile")
            val fourthAsset = storeAsset(client, image, request, path = "user/123/profile/other")

            client.delete("/assets/user/123?mode=recursive").status shouldBe HttpStatusCode.NoContent

            fetchAssetInfo(client, "user/123", entryId = null, HttpStatusCode.NotFound)
            fetchAssetInfo(client, "user/123", firstAsset!!.entryId, HttpStatusCode.NotFound)
            fetchAssetInfo(client, "user/123", secondAsset!!.entryId, HttpStatusCode.NotFound)
            fetchAssetInfo(client, "user/123/profile", thirdAsset!!.entryId, HttpStatusCode.NotFound)
            fetchAssetInfo(client, "user/123/profile/other", fourthAsset!!.entryId, HttpStatusCode.NotFound)

            fetchAssetInfo(client, "user")
            fetchAssetInfo(client, "user", entryId = control!!.entryId)
        }

    @Test
    fun `cannot set both entryId and mode when deleting assets`() =
        testWithTestcontainers(postgres, localstack) {
            client.delete("/assets/user/123?mode=recursive&entryId=1").status shouldBe HttpStatusCode.BadRequest
        }
}

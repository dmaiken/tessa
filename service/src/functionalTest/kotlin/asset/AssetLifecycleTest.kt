package asset

import config.testInMemory
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import org.junit.jupiter.api.Test
import util.createJsonClient
import util.storeAsset
import java.util.UUID

class AssetLifecycleTest {
    @Test
    fun `can create and get image`() =
        testInMemory {
            val client = createJsonClient()
            val image = javaClass.getResourceAsStream("/images/img.png")!!.readBytes()
            val request =
                StoreAssetRequest(
                    fileName = "filename.png",
                    type = "image/png",
                    alt = "an image",
                )
            val storeAssetResponse = storeAsset(client, image, request)
            storeAssetResponse!!.createdAt shouldNotBe null
            storeAssetResponse.variants.first().bucket shouldBe "bucket"
            storeAssetResponse.variants.first().storeKey shouldNotBe null
            storeAssetResponse.variants.first().imageAttributes.mimeType shouldBe "image/png"
            storeAssetResponse.`class` shouldBe AssetClass.IMAGE
            storeAssetResponse.alt shouldBe "an image"
            storeAssetResponse.entryId shouldBe 0
            client.get("/assets/profile?format=metadata").apply {
                status shouldBe HttpStatusCode.OK
                body<AssetResponse>() shouldBe storeAssetResponse
            }
        }

    @Test
    fun `creating asset on same path results in most recent being fetched`() =
        testInMemory {
            val client = createJsonClient()
            val image = javaClass.getResourceAsStream("/images/img.png")!!.readBytes()
            val request =
                StoreAssetRequest(
                    fileName = "filename.png",
                    type = "image/png",
                    alt = "an image",
                )
            val ids = mutableListOf<UUID>()
            repeat(2) {
                val response = storeAsset(client, image, request)
                ids.add(response!!.id)
            }
            ids shouldHaveSize 2
            client.get("/assets/profile?format=metadata").apply {
                status shouldBe HttpStatusCode.OK
                body<AssetResponse>().apply {
                    ids[1] shouldBe id
                    entryId shouldBe 1
                }
            }
        }
}

package asset

import config.testInMemory
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import org.junit.jupiter.api.Test
import util.createJsonClient
import util.storeAsset
import java.util.UUID

class FetchAssetInfoTest {
    @Test
    fun `getting all asset info with path returns all info`() =
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
                storeAsset(client, image, request, path = "profile")?.apply {
                    ids.add(id)
                }
            }
            ids shouldHaveSize 2
            client.get("/assets/profile?format=metadata").apply {
                status shouldBe HttpStatusCode.OK
                body<AssetResponse>().apply {
                    ids[1] shouldBe id
                }
            }

            client.get("/assets/profile?format=metadata&all=true").apply {
                status shouldBe HttpStatusCode.OK
                body<List<AssetResponse>>().apply {
                    size shouldBe 2
                    get(0).id shouldBe ids[1]
                    get(1).id shouldBe ids[0]
                }
            }
        }

    @Test
    fun `fetching info of asset that does not exist returns not found`() =
        testInMemory {
            val client = createJsonClient()
            client.get("/assets/${UUID.randomUUID()}?format=metadata").apply {
                status shouldBe HttpStatusCode.NotFound
            }
        }
}

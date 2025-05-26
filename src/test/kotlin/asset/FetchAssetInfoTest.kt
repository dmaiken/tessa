package io.asset

import asset.StoreAssetRequest
import io.BaseTest
import io.config.testWithTestcontainers
import io.image.AssetResponse
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.util.createJsonClient
import io.util.storeAsset
import org.junit.jupiter.api.Test
import java.util.*

class FetchAssetInfoTest : BaseTest() {

    @Test
    fun `getting all asset info with path returns all info`() =
        testWithTestcontainers(postgres, localstack) {
            val client = createJsonClient()
            val image = javaClass.getResourceAsStream("/images/img.png")!!.readBytes()
            val request = StoreAssetRequest(
                fileName = "filename.png",
                type = "image/png",
                alt = "an image",
            )
            val ids = mutableListOf<UUID>()
            repeat(2) {
                storeAsset(client, image, request, path = "profile").apply {
                    ids.add(id)
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
    fun `fetching info of asset that does not exist returns not found`() =
        testWithTestcontainers(postgres, localstack) {
            val client = createJsonClient()
            client.get("/assets/${UUID.randomUUID()}/info").apply {
                status shouldBe HttpStatusCode.NotFound
            }
        }
}
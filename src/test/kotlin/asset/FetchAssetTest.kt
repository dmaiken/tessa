package io.asset

import asset.StoreAssetRequest
import io.BaseTest
import io.config.testWithTestcontainers
import io.image.byteArrayToImage
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsBytes
import io.ktor.http.HttpStatusCode
import io.util.createGeneralClient
import io.util.createJsonClient
import io.util.storeAsset
import org.apache.tika.Tika
import org.junit.jupiter.api.Test
import java.util.*

class FetchAssetTest : BaseTest() {

    @Test
    fun `fetching asset that does not exist returns not found`() = testWithTestcontainers(postgres, localstack) {
        val client = createJsonClient()
        client.get("/assets/${UUID.randomUUID()}").apply {
            status shouldBe HttpStatusCode.NotFound
        }
    }

    @Test
    fun `can fetch asset and render`() = testWithTestcontainers(postgres, localstack) {
        val client = createJsonClient(followRedirects = false)
        val image = javaClass.getResourceAsStream("/images/img.png")!!.readBytes()
        val bufferedImage = byteArrayToImage(image)
        val request = StoreAssetRequest(
            fileName = "filename.png",
            type = "image/png",
            alt = "an image",
        )
        val storedAssetInfo = storeAsset(client, image, request, path = "profile")

        client.get("/assets/profile/").apply {
            status shouldBe HttpStatusCode.TemporaryRedirect
            headers["Location"] shouldContain "https://"
            headers["Location"] shouldContain storedAssetInfo.storeKey!!

            val generalClient = createGeneralClient()
            val storeResponse = generalClient.get(headers["Location"]!!)
            storeResponse.status shouldBe HttpStatusCode.OK
            val rendered = byteArrayToImage(storeResponse.bodyAsBytes())
            rendered.width shouldBe bufferedImage.width
            rendered.height shouldBe bufferedImage.height
            Tika().detect(storeResponse.bodyAsBytes()) shouldBe "image/png"
        }
    }
}
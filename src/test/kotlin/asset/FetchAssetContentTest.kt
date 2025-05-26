package io.asset

import asset.StoreAssetRequest
import io.BaseTest
import io.config.testWithTestcontainers
import io.image.byteArrayToImage
import io.kotest.matchers.shouldBe
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsBytes
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.util.createJsonClient
import io.util.storeAsset
import org.apache.tika.Tika
import org.junit.jupiter.api.Test
import java.util.*

class FetchAssetContentTest : BaseTest() {

    @Test
    fun `fetching asset content that does not exist returns not found`() =
        testWithTestcontainers(postgres, localstack) {
            val client = createJsonClient()
            client.get("/assets/${UUID.randomUUID()}?format=content").apply {
                status shouldBe HttpStatusCode.NotFound
            }
        }

    @Test
    fun `can fetch asset and render`() = testWithTestcontainers(postgres, localstack) {
        val client = createJsonClient()
        val image = javaClass.getResourceAsStream("/images/img.png")!!.readBytes()
        val bufferedImage = byteArrayToImage(image)
        val request = StoreAssetRequest(
            fileName = "filename.png",
            type = "image/png",
            alt = "an image",
        )
        storeAsset(client, image, request, path = "profile")

        client.get("/assets/profile?format=content").apply {
            status shouldBe HttpStatusCode.OK
            contentType().toString() shouldBe "image/png"
            val imageBytes = bodyAsBytes()
            val rendered = byteArrayToImage(imageBytes)
            rendered.width shouldBe bufferedImage.width
            rendered.height shouldBe bufferedImage.height
            Tika().detect(imageBytes) shouldBe "image/png"
        }
    }
}
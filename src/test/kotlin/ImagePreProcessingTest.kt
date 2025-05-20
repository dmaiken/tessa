package io

import asset.StoreAssetRequest
import io.config.testWithTestcontainers
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.util.createJsonClient
import io.util.storeAsset
import org.junit.jupiter.api.Test

class ImagePreProcessingTest : BaseTest() {

    @Test
    fun `image is resized when it is too large`() = testWithTestcontainers(
        postgres, localstack, mapOf(
            "image.preprocessing.enabled" to "true",
            "image.preprocessing.maxWidth" to "100",
        )
    ) {
        val client = createJsonClient()
        val image = javaClass.getResourceAsStream("/images/img.png")!!.readBytes()
        val request = StoreAssetRequest(
            fileName = "filename.jpeg",
            type = "image/png",
            alt = "an image",
        )
        storeAsset(client, image, request).apply {
            id shouldNotBe null
            createdAt shouldNotBe null
            bucket shouldBe "assets"
            storeKey shouldNotBe null
            type shouldBe "image/png"
            alt shouldBe "an image"
            width shouldBe 100
        }
    }
}
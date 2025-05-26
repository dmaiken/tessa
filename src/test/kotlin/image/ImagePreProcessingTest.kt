package io.image

import asset.StoreAssetRequest
import io.BaseTest
import io.config.testWithTestcontainers
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.matcher.shouldBeApproximately
import io.util.createJsonClient
import io.util.fetchAsset
import io.util.storeAsset
import org.apache.tika.Tika
import org.junit.jupiter.api.Named.named
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.Arguments.arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

class ImagePreProcessingTest : BaseTest() {

    companion object {
        @JvmStatic
        fun scalingNotNeededSource(): Stream<Arguments> = Stream.of(
            arguments(named("No height or width supplied", true), null, null),
            arguments(named("Image preprocessing not enabled", false), 50, 50),
            arguments(named("Height and width are too large", true), 5000, 5000)
        )

        @JvmStatic
        fun imageConversionSource(): Stream<Arguments> = Stream.of(
            arguments("jpeg", "image/jpeg"),
            arguments("jpg", "image/jpeg"),
            arguments("png", "image/png"),
            arguments("webp", "image/webp"),
            arguments("avif", "image/avif")
        )
    }

    @Test
    fun `image width is resized when it is too large`() = testWithTestcontainers(
        postgres, localstack, mapOf(
            "image.preprocessing.enabled" to "true",
            "image.preprocessing.maxWidth" to "100",
        )
    ) {
        val client = createJsonClient(followRedirects = false)
        val image = javaClass.getResourceAsStream("/images/img.png")!!.readBytes()
        val bufferedImage = byteArrayToImage(image)
        val originalScale = bufferedImage.width.toDouble() / bufferedImage.height.toDouble()
        val request = StoreAssetRequest(
            fileName = "filename.png",
            type = "image/png",
            alt = "an image",
        )
        val storedAssetInfo = storeAsset(client, image, request).apply {
            id shouldNotBe null
            createdAt shouldNotBe null
            bucket shouldBe "assets"
            storeKey shouldNotBe null
            type shouldBe "image/png"
            alt shouldBe "an image"
            width shouldBe 100
            width.toDouble() / height.toDouble() shouldBeApproximately originalScale
        }

        val fetchedAsset = fetchAsset(client, storedAssetInfo.storeKey!!)
        Tika().detect(fetchedAsset) shouldBe "image/png"
        val fetchedImage = byteArrayToImage(fetchedAsset)
        fetchedImage.width shouldBe 100
        fetchedImage.width.toDouble() / fetchedImage.height.toDouble() shouldBeApproximately originalScale
    }

    @Test
    fun `image height is resized when it is too large`() = testWithTestcontainers(
        postgres, localstack, mapOf(
            "image.preprocessing.enabled" to "true",
            "image.preprocessing.maxHeight" to "50",
        )
    ) {
        val client = createJsonClient(followRedirects = false)
        val image = javaClass.getResourceAsStream("/images/img.png")!!.readBytes()
        val bufferedImage = byteArrayToImage(image)
        val originalScale = bufferedImage.width.toDouble() / bufferedImage.height.toDouble()
        val request = StoreAssetRequest(
            fileName = "filename.png",
            type = "image/png",
            alt = "an image",
        )
        val storedAssetInfo = storeAsset(client, image, request).apply {
            id shouldNotBe null
            createdAt shouldNotBe null
            bucket shouldBe "assets"
            storeKey shouldNotBe null
            type shouldBe "image/png"
            alt shouldBe "an image"
            height shouldBe 50
            width.toDouble() / height.toDouble() shouldBeApproximately originalScale
        }

        val fetchedAsset = fetchAsset(client, storedAssetInfo.storeKey!!)
        Tika().detect(fetchedAsset) shouldBe "image/png"
        val fetchedImage = byteArrayToImage(fetchedAsset)
        fetchedImage.height shouldBe 50
        fetchedImage.width.toDouble() / fetchedImage.height.toDouble() shouldBeApproximately originalScale
    }

    @ParameterizedTest
    @MethodSource("scalingNotNeededSource")
    fun `image is not resized when not needed`(enabled: Boolean, maxWidth: Int?, maxHeight: Int?) =
        testWithTestcontainers(
            postgres, localstack, buildMap {
                put("image.preprocessing.enabled", enabled.toString())
                maxWidth?.let { put("image.preprocessing.maxWidth", it.toString()) }
                maxHeight?.let { put("image.preprocessing.maxHeight", it.toString()) }
            }
        ) {
            val client = createJsonClient(followRedirects = false)
            val image = javaClass.getResourceAsStream("/images/img.png")!!.readBytes()
            val bufferedImage = byteArrayToImage(image)
            val request = StoreAssetRequest(
                fileName = "filename.png",
                type = "image/png",
                alt = "an image",
            )
            val storedAssetInfo = storeAsset(client, image, request).apply {
                id shouldNotBe null
                createdAt shouldNotBe null
                bucket shouldBe "assets"
                storeKey shouldNotBe null
                type shouldBe "image/png"
                alt shouldBe "an image"
                width shouldBe bufferedImage.width
                height shouldBe bufferedImage.height
            }
            val fetchedAsset = fetchAsset(client, storedAssetInfo.storeKey!!)
            Tika().detect(fetchedAsset) shouldBe "image/png"
            val fetchedImage = byteArrayToImage(fetchedAsset)
            fetchedImage.width shouldBe bufferedImage.width
            fetchedImage.height shouldBe bufferedImage.height
        }

    @ParameterizedTest
    @MethodSource("imageConversionSource")
    fun `image is converted if necessary`(imageFormat: String, expectedType: String) = testWithTestcontainers(
        postgres, localstack, mapOf(
            "image.preprocessing.enabled" to "true",
            "image.preprocessing.imageFormat" to imageFormat,
        )
    ) {
        val client = createJsonClient(followRedirects = false)
        val image = javaClass.getResourceAsStream("/images/img.png")!!.readBytes()
        val bufferedImage = byteArrayToImage(image)
        val request = StoreAssetRequest(
            fileName = "filename.png",
            type = "image/png",
            alt = "an image",
        )
        val storedAssetInfo = storeAsset(client, image, request).apply {
            id shouldNotBe null
            createdAt shouldNotBe null
            bucket shouldBe "assets"
            storeKey shouldNotBe null
            type shouldBe expectedType
            alt shouldBe "an image"
            width shouldBe bufferedImage.width
            height shouldBe bufferedImage.height
        }
        val fetchedAsset = fetchAsset(client, storedAssetInfo.storeKey!!)
        Tika().detect(fetchedAsset) shouldBe expectedType
    }
}

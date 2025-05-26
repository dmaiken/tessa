package io.image

import app.photofox.vipsffm.VImage
import app.photofox.vipsffm.Vips
import io.ktor.util.logging.KtorSimpleLogger
import org.apache.commons.io.output.ByteArrayOutputStream
import kotlin.math.min

interface ImageProcessor {
    suspend fun preprocess(image: ByteArray, mimeType: String): ProcessedImage
}

class VipsImageProcessor(
    private val imageProperties: ImageProperties
) : ImageProcessor {

    private val logger = KtorSimpleLogger(this::class.qualifiedName!!)

    override suspend fun preprocess(image: ByteArray, mimeType: String): ProcessedImage {
        var attributes: ImageAttributes? = null
        val resizedStream = ByteArrayOutputStream()
        Vips.run { arena ->
            val sourceImage = VImage.newFromBytes(arena, image)
            if (!imageProperties.preProcessing.enabled) {
                attributes = ImageAttributes(
                    height = sourceImage.height,
                    width = sourceImage.width,
                )
                resizedStream.write(image)
                return@run
            }

            val resized = downScale(
                image = sourceImage,
                maxWidth = imageProperties.preProcessing.maxWidth,
                maxHeight = imageProperties.preProcessing.maxHeight
            )
            resized.writeToStream(resizedStream, ".${mimeType.split("/")[1]}")
            attributes = ImageAttributes(
                height = resized.height,
                width = resized.width,
            )
        }
        return ProcessedImage(
            image = resizedStream.toByteArray(),
            attributes = attributes ?: throw IllegalStateException()
        )
    }

    /**
     * Downscale the image to fit within the given max width and height. Height or width may be smaller based on the
     * image's aspect ratio. If both maxWidth and maxHeight are null, the image is not downscaled.
     */
    private fun downScale(image: VImage, maxWidth: Int?, maxHeight: Int?): VImage {
        if (maxWidth == null && maxHeight == null) {
            logger.info("Preprocessing width and height are not set, skipping preprocessing downscaling")
            return image
        }
        // Compute scale so that the image fits within max dimensions
        val widthRatio = maxWidth?.let {
            it.toDouble() / image.width
        } ?: 1.0
        val heightRatio = maxHeight?.let {
            it.toDouble() / image.height
        } ?: 1.0
        val scale = min(widthRatio, heightRatio)

        // Don't upscale
        if (scale >= 1.0) return image

        logger.info("Scaling image to $scale based on max width $maxWidth and max height $maxHeight")

        return image.resize(scale)
    }
}

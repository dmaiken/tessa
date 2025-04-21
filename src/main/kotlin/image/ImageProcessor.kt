package io.image

import app.photofox.vipsffm.VImage
import app.photofox.vipsffm.Vips
import org.apache.commons.io.output.ByteArrayOutputStream

interface ImageProcessor {
    suspend fun preprocess(image: ByteArray, mimeType: String): ProcessedImage
}

class VipsImageProcessor(
    private val imageProperties: ImageProperties
) : ImageProcessor {

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

            val resized = downScaleWidth(sourceImage, imageProperties.preProcessing.maxWidth)
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

    private fun downScaleWidth(image: VImage, width: Int): VImage {
        if (image.width <= width) {
            return image
        }
        val scale = width.toDouble() / image.width
        return image.resize(scale)
    }
}

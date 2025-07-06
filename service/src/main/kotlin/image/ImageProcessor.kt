package image

import app.photofox.vipsffm.VImage
import app.photofox.vipsffm.Vips
import asset.variant.ImageVariantAttributes
import image.model.ImageAttributes
import image.model.ImageFormat
import image.model.PreProcessingProperties
import image.model.ProcessedImage
import image.model.RequestedImageAttributes
import io.ktor.util.logging.KtorSimpleLogger
import io.path.configuration.PathConfiguration
import java.io.InputStream
import java.io.PipedOutputStream
import kotlin.math.min

interface ImageProcessor {
    /**
     * Preprocesses the image based on application configuration. Make sure to use the returned properties
     * since they reflect any changes performed on the image.
     */
    suspend fun preprocess(
        image: ByteArray,
        mimeType: String,
        pathConfiguration: PathConfiguration,
    ): ProcessedImage

    suspend fun generateFrom(
        from: InputStream,
        requestedAttributes: RequestedImageAttributes,
        generatedFromAttributes: ImageVariantAttributes,
    ): ProcessedImage
}

class VipsImageProcessor() : ImageProcessor {
    private val logger = KtorSimpleLogger(this::class.qualifiedName!!)

    override suspend fun preprocess(
        image: ByteArray,
        mimeType: String,
        pathConfiguration: PathConfiguration,
    ): ProcessedImage {
        var attributes: ImageAttributes? = null
        val resizedStream = PipedOutputStream()
        val preProcessingProperties =
            pathConfiguration.imageProperties.preProcessing
        Vips.run { arena ->
            val sourceImage = VImage.newFromBytes(arena, image)
            if (!preProcessingProperties.enabled) {
                attributes =
                    ImageAttributes(
                        height = sourceImage.height,
                        width = sourceImage.width,
                        mimeType = mimeType,
                    )
                resizedStream.write(image)
                return@run
            }

            val resized =
                scale(
                    image = sourceImage,
                    width = preProcessingProperties.maxWidth,
                    height = preProcessingProperties.maxHeight,
                )
            val newMimeType = determineMimeType(mimeType, preProcessingProperties)
            resized.writeToStream(resizedStream, ".${ImageFormat.fromMimeType(newMimeType).extension}")
            attributes =
                ImageAttributes(
                    height = resized.height,
                    width = resized.width,
                    mimeType = newMimeType,
                )
        }
        return ProcessedImage(
            output = resizedStream,
            attributes = attributes ?: throw IllegalStateException(),
        )
    }

    override suspend fun generateFrom(
        from: InputStream,
        requestedAttributes: RequestedImageAttributes,
        generatedFromAttributes: ImageVariantAttributes,
    ): ProcessedImage {
        var attributes: ImageAttributes? = null
        val output = PipedOutputStream()
        Vips.run { arena ->
            val image = VImage.newFromStream(arena, from)
            // Determine if we need to downscale or upscale
            val resized =
                scale(
                    image = image,
                    width = requestedAttributes.width,
                    height = requestedAttributes.height,
                )
            val mimeType = requestedAttributes.mimeType ?: generatedFromAttributes.mimeType
            val imageFormat = ImageFormat.fromMimeType(mimeType)

            resized.writeToStream(output, ".${imageFormat.extension}")

            attributes =
                ImageAttributes(
                    width = resized.width,
                    height = resized.height,
                    mimeType = mimeType,
                )
        }

        return ProcessedImage(
            output = output,
            attributes = attributes ?: throw IllegalStateException(),
        )
    }

    /**
     * Scales the image to fit within the given max width and height. Height or width may be smaller based on the
     * image's aspect ratio. If both maxWidth and maxHeight are null, the image is not downscaled.
     */
    private fun scale(
        image: VImage,
        width: Int?,
        height: Int?,
    ): VImage {
        if (width == null && height == null) {
            logger.info("Preprocessing width and height are not set, skipping preprocessing downscaling")
            return image
        }
        // Compute scale so that the image fits within max dimensions
        val widthRatio =
            width?.let {
                it.toDouble() / image.width
            } ?: 1.0
        val heightRatio =
            height?.let {
                it.toDouble() / image.height
            } ?: 1.0
        val scale = min(widthRatio, heightRatio)

        // Don't upscale
        if (scale >= 1.0) return image

        logger.info("Scaling image to $scale based on max width $width and max height $height")

        return image.resize(scale)
    }

    private fun determineMimeType(
        originalMimeType: String,
        preProcessingProperties: PreProcessingProperties,
    ) = if (preProcessingProperties.imageFormat != null) {
        if (preProcessingProperties.imageFormat.mimeType != originalMimeType) {
            logger.info("Converting image from $originalMimeType to ${preProcessingProperties.imageFormat.mimeType}")
        }
        preProcessingProperties.imageFormat.mimeType
    } else {
        originalMimeType
    }
}

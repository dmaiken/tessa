package io.image

import io.image.PreProcessingProperties.Companion.ENABLED
import io.image.PreProcessingProperties.Companion.IMAGE_FORMAT
import io.image.PreProcessingProperties.Companion.MAX_HEIGHT
import io.image.PreProcessingProperties.Companion.MAX_WIDTH
import io.ktor.server.config.ApplicationConfig
import io.properties.ValidatedProperties
import io.properties.validateAndCreate

class ImageProperties private constructor(
    val preProcessing: PreProcessingProperties,
) : ValidatedProperties {
    override fun validate() {}

    companion object {
        fun create(preProcessing: PreProcessingProperties) = validateAndCreate { ImageProperties(preProcessing) }
    }
}

class PreProcessingProperties private constructor(
    val enabled: Boolean,
    val maxWidth: Int?,
    val maxHeight: Int?,
    val imageFormat: ImageFormat?,
) : ValidatedProperties {
    override fun validate() {
        maxWidth?.let {
            require(it > 0) { "'${MAX_WIDTH}' must be greater than 0" }
        }
        maxHeight?.let {
            require(it > 0) { "'${MAX_HEIGHT}' must be greater than 0" }
        }
    }

    companion object {
        const val ENABLED = "enabled"
        const val MAX_HEIGHT = "max-height"
        const val MAX_WIDTH = "max-width"
        const val IMAGE_FORMAT = "image-format"

        fun create(
            enabled: Boolean,
            maxWidth: Int?,
            maxHeight: Int?,
            imageFormat: ImageFormat?,
        ) = validateAndCreate { PreProcessingProperties(enabled, maxWidth, maxHeight, imageFormat) }
    }
}

fun constructImageProperties(config: ApplicationConfig?): ImageProperties {
    return ImageProperties.create(
        preProcessing =
            PreProcessingProperties.create(
                enabled =
                    config?.propertyOrNull("preprocessing.$ENABLED")?.getString()
                        ?.toBoolean()
                        ?: false,
                maxWidth =
                    config?.propertyOrNull("preprocessing.$MAX_WIDTH")?.getString()
                        ?.toInt(),
                maxHeight =
                    config?.propertyOrNull("preprocessing.$MAX_HEIGHT")?.getString()
                        ?.toInt(),
                imageFormat =
                    config?.propertyOrNull("preprocessing.$IMAGE_FORMAT")?.getString()
                        ?.let {
                            ImageFormat.fromFormat(it)
                        },
            ),
    )
}

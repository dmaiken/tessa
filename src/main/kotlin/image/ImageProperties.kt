package io.image

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
            require(it > 0) { "'$PreProcessingProperties' must be greater than 0" }
        }
        maxHeight?.let {
            require(it > 0) { "'maxHeight' must be greater than 0" }
        }
    }

    companion object {
        const val ENABLED = "enabled"
        const val MAX_HEIGHT = "max-height"
        const val MAX_WIDTH = "max-width"

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
                    config?.propertyOrNull("preprocessing.enabled")?.getString()
                        ?.toBoolean()
                        ?: false,
                maxWidth =
                    config?.propertyOrNull("preprocessing.maxWidth")?.getString()
                        ?.toInt(),
                maxHeight =
                    config?.propertyOrNull("preprocessing.maxHeight")?.getString()
                        ?.toInt(),
                imageFormat =
                    config?.propertyOrNull("preprocessing.imageFormat")?.getString()
                        ?.let {
                            ImageFormat.fromFormat(it)
                        },
            ),
    )
}

package io.image

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
            require(it > 0) { "'maxWidth' must be greater than 0" }
        }
        maxHeight?.let {
            require(it > 0) { "'maxHeight' must be greater than 0" }
        }
    }

    companion object {
        fun create(
            enabled: Boolean,
            maxWidth: Int?,
            maxHeight: Int?,
            imageFormat: ImageFormat?,
        ) = validateAndCreate { PreProcessingProperties(enabled, maxWidth, maxHeight, imageFormat) }
    }
}

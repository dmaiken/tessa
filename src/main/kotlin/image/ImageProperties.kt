package io.image

data class ImageProperties(
    val preProcessing: PreProcessingProperties,
)

data class PreProcessingProperties(
    val enabled: Boolean = false,
    val maxWidth: Int,

    )

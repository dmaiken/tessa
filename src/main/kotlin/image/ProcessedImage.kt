package io.image

data class ProcessedImage(
    val image: ByteArray,
    val attributes: ImageAttributes
)

data class ImageAttributes(
    val width: Int,
    val height: Int,
)

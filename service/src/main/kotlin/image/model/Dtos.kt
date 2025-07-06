package image.model

import java.io.PipedOutputStream

data class ProcessedImage(
    val output: PipedOutputStream,
    val attributes: ImageAttributes,
)

data class RequestedImageAttributes(
    val width: Int?,
    val height: Int?,
    val mimeType: String?,
) {
    companion object Factory {
        fun originalVariant(): RequestedImageAttributes =
            RequestedImageAttributes(
                width = null,
                height = null,
                mimeType = null,
            )
    }

    fun isOriginalVariant(): Boolean {
        return width == null && height == null && mimeType == null
    }
}

data class ImageAttributes(
    val width: Int,
    val height: Int,
    val mimeType: String,
) {
    fun toRequestedAttributes(): RequestedImageAttributes {
        return RequestedImageAttributes(
            width = width,
            height = height,
            mimeType = mimeType,
        )
    }
}

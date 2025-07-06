package asset.variant

import io.image.ImageAttributes
import kotlinx.serialization.json.Json

class VariantParameterGenerator {
    fun generateImageVariantAttributes(imageAttributes: ImageAttributes): String {
        return Json.encodeToString(
            ImageVariantAttributes(
                height = imageAttributes.height,
                width = imageAttributes.width,
                mimeType = imageAttributes.mimeType,
            ),
        )
    }
}

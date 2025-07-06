package asset.variant

import image.model.RequestedImageAttributes
import io.ktor.util.logging.KtorSimpleLogger
import kotlinx.serialization.json.Json

class VariantParameterGenerator {
    private val logger = KtorSimpleLogger(this::class.qualifiedName!!)

    fun generateImageVariantAttributes(requestedImageAttributes: RequestedImageAttributes): VariantAttributesAndKey {
        val attributes =
            Json.encodeToString(
                ImageVariantAttributes(
                    height = checkNotNull(requestedImageAttributes.height),
                    width = checkNotNull(requestedImageAttributes.width),
                    mimeType = checkNotNull(requestedImageAttributes.mimeType),
                ),
            )
        logger.info("Generated attributes: $attributes")
        return VariantAttributesAndKey(
            attributes = attributes,
            key = generateAttributesKey(attributes),
        )
    }

    private fun generateAttributesKey(attributes: String): ByteArray {
        return attributes.toByteArray() // TODO hash this
    }
}

data class VariantAttributesAndKey(
    val attributes: String,
    val key: ByteArray,
)

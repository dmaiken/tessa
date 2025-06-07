package io.path.configuration

import io.WildcardRegexAdapter
import io.image.ImageFormat
import io.image.ImageProperties
import io.properties.ValidatedProperties

class PathConfiguration private constructor(
    val pathMatcher: String,
    val pathRegex: Regex,
    val allowedContentTypes: List<String>?,
    val allowedContentTypesRegex: List<Regex>?,
    val imageProperties: ImageProperties,
) : ValidatedProperties {
    companion object {
        const val PATH_MATCHER = "path-matcher"
        const val ALLOWED_CONTENT_TYPES = "allowed-content-types"

        fun create(
            pathMatcher: String?,
            allowedContentTypes: List<String>?,
            imageProperties: ImageProperties,
        ): PathConfiguration {
            val wildcardRegexAdapter = WildcardRegexAdapter()
            return PathConfiguration(
                pathMatcher =
                    requireNotNull(pathMatcher) {
                        "Path matcher is required"
                    }.lowercase(),
                pathRegex = wildcardRegexAdapter.toRegex(pathMatcher),
                allowedContentTypes = allowedContentTypes?.map { it.lowercase() },
                allowedContentTypesRegex =
                    allowedContentTypes?.let { contentType ->
                        wildcardRegexAdapter.toRegexList(contentType.map { it.lowercase() })
                    },
                imageProperties = imageProperties,
            ).also {
                it.validate()
            }
        }
    }

    override fun validate() {
        require(pathMatcher.isNotBlank()) {
            "pathMatcher must not be empty or blank"
        }
        allowedContentTypes?.let { allowedContentTypes ->
            val supportedContentTypes = ImageFormat.entries.map { it.mimeType }
            allowedContentTypes.forEach { allowedContentType ->
                require(supportedContentTypes.contains(allowedContentType)) {
                    "$allowedContentType is not a supported content type"
                }
            }
        }
    }
}

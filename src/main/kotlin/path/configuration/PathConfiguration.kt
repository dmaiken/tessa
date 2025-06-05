package io.path.configuration

import java.time.LocalDateTime
import java.util.UUID

data class PathConfiguration(
    val id: UUID = UUID.randomUUID(),
    val pathMatcher: String,
    val pathRegex: Regex,
    val allowedContentTypes: List<String>,
    val allowedContentTypesRegex: List<Regex>,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val modifiedAt: LocalDateTime = LocalDateTime.now(),
) {
    fun toResponse(): PathConfigurationResponse =
        PathConfigurationResponse(
            pathMatcher = pathMatcher,
            allowedContentTypes = allowedContentTypes,
            createdAt = createdAt,
            modifiedAt = modifiedAt,
        )
}

package io.path.configuration

import io.image.constructImageProperties
import io.ktor.server.config.ApplicationConfig
import io.ktor.server.config.tryGetString
import io.ktor.server.config.tryGetStringList
import io.path.configuration.PathConfiguration.Companion.ALLOWED_CONTENT_TYPES
import io.path.configuration.PathConfiguration.Companion.PATH_MATCHER
import io.properties.ConfigurationProperties
import io.tryGetConfig

class PathConfigurationService(
    applicationConfig: ApplicationConfig,
) {
    private val pathConfigurations: List<PathConfiguration> =
        applicationConfig.configList(ConfigurationProperties.PATH_CONFIGURATION).map { config ->
            PathConfiguration.create(
                allowedContentTypes = config.tryGetStringList(ALLOWED_CONTENT_TYPES),
                pathMatcher =
                    config.tryGetString(PATH_MATCHER),
                imageProperties = constructImageProperties(config.tryGetConfig("image")),
            )
        }

    fun fetch(uriPath: String): PathConfiguration? {
        return uriPath.lowercase().let { path ->
            pathConfigurations.firstOrNull { config -> config.pathRegex.matches(path) }
        }
    }
}

package io.path.configuration

import io.WildcardRegexAdapter
import io.ktor.server.config.ApplicationConfig
import io.ktor.server.config.tryGetString
import io.ktor.server.config.tryGetStringList
import io.ktor.util.logging.KtorSimpleLogger

class PathConfigurationService(
    applicationConfig: ApplicationConfig,
) {
    private val logger = KtorSimpleLogger(this::class.qualifiedName!!)
    private val wildcardRegexAdapter = WildcardRegexAdapter()

    private val pathConfigurations: List<PathConfiguration> =
        applicationConfig.configList("path-configuration").map { config ->
            PathConfiguration(
                allowedContentTypesRegex =
                    config.tryGetStringList("allowed-content-types")
                        ?.map { wildcardRegexAdapter.toRegex(it) } ?: emptyList(),
                allowedContentTypes = config.tryGetStringList("allowed-content-types") ?: emptyList(),
                pathMatcher =
                    config.tryGetString("path-matcher")
                        ?: throw IllegalArgumentException("Path matcher is required"),
                pathRegex =
                    config.tryGetString("path-matcher")?.let {
                        wildcardRegexAdapter.toRegex(it)
                    } ?: throw IllegalArgumentException("Path matcher is required"),
            )
        }

    fun fetch(uriPath: String): PathConfiguration? {
        return uriPath.removePrefix("/assets").lowercase().let { path ->
            pathConfigurations.firstOrNull { config -> config.pathRegex.matches(path) }
        }
    }
}

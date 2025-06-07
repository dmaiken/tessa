package io.path

import io.image.ImageProperties
import io.image.PreProcessingProperties
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.path.configuration.PathConfiguration
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class PathConfigurationTest {
    @Test
    fun `unsupported content type is not allowed`() {
        val exception =
            shouldThrow<IllegalArgumentException> {
                PathConfiguration.create(
                    pathMatcher = "profile",
                    allowedContentTypes = listOf("not/supported"),
                    imageProperties =
                        ImageProperties.create(
                            PreProcessingProperties.create(
                                enabled = false,
                                maxWidth = null,
                                maxHeight = null,
                                imageFormat = null,
                            ),
                        ),
                )
            }

        exception.message shouldBe "not/supported is not a supported content type"
    }

    @ParameterizedTest
    @ValueSource(strings = ["", " "])
    fun `pathMatcher must have characters`(pathMatcher: String) {
        val exception =
            shouldThrow<IllegalArgumentException> {
                PathConfiguration.create(
                    pathMatcher = pathMatcher,
                    allowedContentTypes = null,
                    imageProperties =
                        ImageProperties.create(
                            PreProcessingProperties.create(
                                enabled = false,
                                maxWidth = null,
                                maxHeight = null,
                                imageFormat = null,
                            ),
                        ),
                )
            }

        exception.message shouldBe "pathMatcher must not be empty or blank"
    }

    @Test
    fun `pathMatcher must not be null`() {
        val exception =
            shouldThrow<IllegalArgumentException> {
                PathConfiguration.create(
                    pathMatcher = null,
                    allowedContentTypes = null,
                    imageProperties =
                        ImageProperties.create(
                            PreProcessingProperties.create(
                                enabled = false,
                                maxWidth = null,
                                maxHeight = null,
                                imageFormat = null,
                            ),
                        ),
                )
            }

        exception.message shouldBe "Path matcher is required"
    }
}

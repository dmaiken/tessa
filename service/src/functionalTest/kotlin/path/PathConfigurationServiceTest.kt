package path

import config.testInMemory
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.path.configuration.PathConfigurationService
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class PathConfigurationServiceTest {
    @Test
    fun `fetch returns a path configuration when the path matches exactly`() =
        testInMemory(
            """
            path-configuration = [
              {
                path = "/users/123/profile"
                allowed-content-types = [
                  "image/png",
                  "image/jpeg"
                ]
              },
              {
                path = "/users/456/profile"
                allowed-content-types = [
                  "image/jpeg"
                ]
              }
            ]
            """.trimIndent(),
        ) {
            application {
                val pathConfigurationService = PathConfigurationService(environment.config)
                val pathConfiguration = pathConfigurationService.fetchConfigurationForPath("/users/123/profile")
                pathConfiguration.allowedContentTypes shouldBe listOf("image/png", "image/jpeg")
            }
        }

    @Test
    fun `fetch returns a path configuration when the path matches exactly but case does not`() =
        testInMemory(
            """
            path-configuration = [
              {
                path = "/Users/123/Profile"
                allowed-content-types = [
                  "image/png",
                  "image/jpeg"
                ]
              },
              {
                path = "/users/456/profile"
                allowed-content-types = [
                  "image/jpeg"
                ]
              }
            ]
            """.trimIndent(),
        ) {
            application {
                val pathConfigurationService = PathConfigurationService(environment.config)
                listOf(
                    "/users/123/profile",
                    "/USERS/123/profile",
                ).forEach { path ->
                    val pathConfiguration = pathConfigurationService.fetchConfigurationForPath(path)
                    pathConfiguration.allowedContentTypes shouldBe listOf("image/png", "image/jpeg")
                }
            }
        }

    @Test
    fun `fetch returns a path configuration when the path matcher has single wildcard`() =
        testInMemory(
            """
            path-configuration = [
              {
                path = "/users/*/profile"
                allowed-content-types = [
                  "image/png",
                  "image/jpeg"
                ]
              }
            ]
            """.trimIndent(),
        ) {
            application {
                val pathConfigurationService = PathConfigurationService(environment.config)
                val pathConfiguration = pathConfigurationService.fetchConfigurationForPath("/users/123/profile")
                pathConfiguration.allowedContentTypes shouldBe listOf("image/png", "image/jpeg")
            }
        }

    @Test
    fun `fetch returns a path configuration when the path matcher has double wildcard`() =
        testInMemory(
            """
            path-configuration = [
              {
                path = "/users/**"
                allowed-content-types = [
                  "image/png",
                  "image/jpeg"
                ]
              }
            ]
            """.trimIndent(),
        ) {
            application {
                val pathConfigurationService = PathConfigurationService(environment.config)
                val pathConfiguration = pathConfigurationService.fetchConfigurationForPath("/users/123/profile")
                pathConfiguration.allowedContentTypes shouldBe listOf("image/png", "image/jpeg")
            }
        }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "/users/123/profile",
            "/users/*/profile",
            "/users/**",
        ],
    )
    fun `fetch does not return a path configuration when the path matcher does not match`(path: String) =
        testInMemory(
            """
            path-configuration = [
              {
                path = "$path"
                allowed-content-types = [
                  "image/png",
                  "image/jpeg"
                ]
              }
            ]
            """.trimIndent(),
        ) {
            application {
                val pathConfigurationService = PathConfigurationService(environment.config)
                pathConfigurationService.fetchConfigurationForPath("/notAUser/123/profile").apply {
                    imageProperties.apply {
                        preProcessing.enabled shouldBe false
                    }
                    allowedContentTypes shouldBe null
                }
            }
        }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "/users/**/profile/**",
            "/users/**",
            "/users/**/profile/**/last",
        ],
    )
    fun `greedy wildcard matching works`(path: String) =
        testInMemory(
            """
            path-configuration = [
              {
                path = "$path"
                allowed-content-types = [
                  "image/png",
                  "image/jpeg"
                ]
              }
            ]
            """.trimIndent(),
        ) {
            application {
                val pathConfigurationService = PathConfigurationService(environment.config)
                val pathConfiguration =
                    pathConfigurationService.fetchConfigurationForPath("/users/lastName/firstName/profile/last")
                pathConfiguration.allowedContentTypes shouldBe listOf("image/png", "image/jpeg")
            }
        }

    @Test
    fun `path configuration is inherited if not supplied`() =
        testInMemory(
            """
            path-configuration = [
              {
                path = "/users/*"
                allowed-content-types = [
                  "image/png",
                  "image/jpeg"
                ],
                image {
                  preprocessing = {
                    max-height = 10
                  }
                }
              },
              {
                path = "/users/*/profile"
                allowed-content-types = [ ]
                image {
                  preprocessing = {
                    max-width = 10
                  }
                }
              },
            ]
            """.trimIndent(),
        ) {
            application {
                val pathConfigurationService = PathConfigurationService(environment.config)
                val pathConfiguration = pathConfigurationService.fetchConfigurationForPath("/users/123/profile")
                pathConfiguration.allowedContentTypes shouldBe listOf()
                pathConfiguration.imageProperties.preProcessing.maxWidth shouldBe 10
                pathConfiguration.imageProperties.preProcessing.maxHeight shouldBe 10
            }
        }

    @Test
    fun `default path is used when none suffice`() =
        testInMemory(
            """
            path-configuration = [
              {
                path = "/**"
                allowed-content-types = [
                  "image/png",
                  "image/jpeg"
                ]
              },
              {
                path = "/users/**"
                allowed-content-types = [
                ]
              }
            ]
            """.trimIndent(),
        ) {
            application {
                val pathConfigurationService = PathConfigurationService(environment.config)
                val pathConfiguration = pathConfigurationService.fetchConfigurationForPath("/recipe/123")
                pathConfiguration.allowedContentTypes shouldBe listOf("image/png", "image/jpeg")
            }
        }

    @Test
    fun `default path configuration is inherited`() =
        testInMemory(
            """
            path-configuration = [
              {
                path = "/**"
                allowed-content-types = [
                  "image/png",
                  "image/jpeg"
                ],
                image {
                  preprocessing = {
                    max-height = 10
                  }
                }
              },
              {
                path = "/users/*/profile"
                allowed-content-types = [ ]
                image {
                  preprocessing = {
                    max-width = 10
                  }
                }
              },
            ]
            """.trimIndent(),
        ) {
            application {
                val pathConfigurationService = PathConfigurationService(environment.config)
                val pathConfiguration = pathConfigurationService.fetchConfigurationForPath("/users/123/profile")
                pathConfiguration.allowedContentTypes shouldBe listOf()
                pathConfiguration.imageProperties.preProcessing.maxWidth shouldBe 10
                pathConfiguration.imageProperties.preProcessing.maxHeight shouldBe 10
            }
        }

    @Test
    fun `path is stripped of blank and empty path segments`() =
        testInMemory(
            """
            path-configuration = [
              {
                path = "/**"
                allowed-content-types = [
                  "image/png",
                  "image/jpeg"
                ]
              }
            ]
            """.trimIndent(),
        ) {
            application {
                val pathConfigurationService = PathConfigurationService(environment.config)
                val pathConfiguration = pathConfigurationService.fetchConfigurationForPath("// //123")
                pathConfiguration.allowedContentTypes shouldBe listOf("image/png", "image/jpeg")
            }
        }

    @Test
    fun `path must be supplied`() =
        testInMemory(
            """
            path-configuration = [
              {
                allowed-content-types = [
                  "image/png",
                  "image/jpeg"
                ]
              }
            ]
            """.trimIndent(),
        ) {
            application {
                val exception =
                    shouldThrow<IllegalArgumentException> {
                        PathConfigurationService(environment.config)
                    }
                exception.message shouldBe "Path configuration must be supplied"
            }
        }
}

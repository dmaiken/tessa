package io.path

import io.BaseTest
import io.config.testWithTestcontainers
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.path.configuration.PathConfigurationService
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class PathConfigurationServiceTest : BaseTest() {
    @Test
    fun `fetch returns a path configuration when the path matches exactly`() =
        testWithTestcontainers(
            postgres,
            localstack,
            """
            path-configuration = [
              {
                path-matcher = "/users/123/profile"
                allowed-content-types = [
                  "image/png",
                  "image/jpeg"
                ]
              },
              {
                path-matcher = "/users/456/profile"
                allowed-content-types = [
                  "image/jpeg"
                ]
              }
            ]
            """.trimIndent(),
        ) {
            application {
                val pathConfigurationService = PathConfigurationService(environment.config)
                val pathConfiguration = pathConfigurationService.fetch("/users/123/profile")
                pathConfiguration shouldNotBe null
                pathConfiguration?.allowedContentTypes shouldBe listOf("image/png", "image/jpeg")
            }
        }

    @Test
    fun `fetch returns a path configuration when the path matches exactly but case does not`() =
        testWithTestcontainers(
            postgres,
            localstack,
            """
            path-configuration = [
              {
                path-matcher = "/Users/123/Profile"
                allowed-content-types = [
                  "image/png",
                  "image/jpeg"
                ]
              },
              {
                path-matcher = "/users/456/profile"
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
                    val pathConfiguration = pathConfigurationService.fetch(path)
                    pathConfiguration shouldNotBe null
                    pathConfiguration?.allowedContentTypes shouldBe listOf("image/png", "image/jpeg")
                }
            }
        }

    @Test
    fun `fetch returns a path configuration when the path matcher has single wildcard`() =
        testWithTestcontainers(
            postgres,
            localstack,
            """
            path-configuration = [
              {
                path-matcher = "/users/*/profile"
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
                val pathConfiguration = pathConfigurationService.fetch("/users/123/profile")
                pathConfiguration shouldNotBe null
                pathConfiguration?.allowedContentTypes shouldBe listOf("image/png", "image/jpeg")
            }
        }

    @Test
    fun `fetch returns a path configuration when the path matcher has double wildcard`() =
        testWithTestcontainers(
            postgres,
            localstack,
            """
            path-configuration = [
              {
                path-matcher = "/users/**"
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
                val pathConfiguration = pathConfigurationService.fetch("/users/123/profile")
                pathConfiguration shouldNotBe null
                pathConfiguration?.allowedContentTypes shouldBe listOf("image/png", "image/jpeg")
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
        testWithTestcontainers(
            postgres,
            localstack,
            """
            path-configuration = [
              {
                path-matcher = "$path"
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
                pathConfigurationService.fetch("/assets/notAUser/123/profile") shouldBe null
            }
        }
}

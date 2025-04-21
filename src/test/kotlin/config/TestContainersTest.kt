package io.config

import com.typesafe.config.ConfigFactory
import io.ktor.server.config.*
import io.ktor.server.testing.*

fun testWithTestcontainers(
    postgres: PostgresTestContainerManager,
    localstack: LocalstackContainerManager,
    configuration: Map<String, String> = mapOf(),
    testBody: suspend ApplicationTestBuilder.() -> Unit
) {
    testApplication {
        environment {
            val mergedConfiguration = configuration.map {
                Pair(it.key, it.value)
            } + listOf(
                "postgres.port" to postgres.getPort().toString(),
                "aws.mock" to "true",
                "localstack.region" to localstack.getRegion(),
                "localstack.accessKey" to localstack.getAccessKey(),
                "localstack.secretKey" to localstack.getSecretKey(),
                "localstack.endpointUrl" to localstack.getEndpointUrl(),
                "localstack.port" to localstack.getPort().toString()
            )
            config = HoconApplicationConfig(ConfigFactory.load()).mergeWith(
                MapApplicationConfig(mergedConfiguration)
            )
        }
        testBody()
    }
}
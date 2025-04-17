package io

import com.typesafe.config.ConfigFactory
import io.ktor.server.config.*
import io.ktor.server.testing.*

fun testWithTestcontainers(
    postgres: PostgresTestContainerManager,
    localstack: LocalstackContainerManager,
    testBody: suspend ApplicationTestBuilder.() -> Unit
) {
    testApplication {
        environment {
            config = HoconApplicationConfig(ConfigFactory.load()).mergeWith(
                MapApplicationConfig(
                    "postgres.port" to postgres.getPort().toString(),
                    "aws.mock" to "true",
                    "localstack.region" to localstack.getRegion(),
                    "localstack.accessKey" to localstack.getAccessKey(),
                    "localstack.secretKey" to localstack.getSecretKey(),
                    "localstack.endpointUrl" to localstack.getEndpointUrl()
                )
            )
        }
        testBody()
    }
}
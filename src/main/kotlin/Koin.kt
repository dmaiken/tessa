package io

import aws.sdk.kotlin.services.s3.S3Client
import io.image.AssetService
import io.image.AssetServiceImpl
import io.image.S3Service
import io.image.store.ObjectStore
import io.ktor.server.application.*
import io.r2dbc.spi.ConnectionFactory
import org.jooq.DSLContext
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger

fun Application.configureKoin(
    connectionFactory: ConnectionFactory,
    localstackProperties: LocalstackProperties? = null
) {
    val appModule = module {
        single<AssetService> {
            AssetServiceImpl(get(), get())
        }
    }
    val dbModule = module {
        single<ConnectionFactory> {
            migrateSchema(connectionFactory)
            connectionFactory
        }
        single<DSLContext> {
            configureJOOQ(get())
        }
    }

    val awsModule = module {
        single<S3Client> {
            val useMock = environment.config.propertyOrNull("aws.mock")?.getString().toBoolean() ?: false
            if (useMock) {
                s3Client(
                    LocalstackProperties(
                        region = environment.config.property("localstack.region").getString(),
                        accessKey = environment.config.property("localstack.accessKey").getString(),
                        secretKey = environment.config.property("localstack.secretKey").getString(),
                        endpointUrl = environment.config.property("localstack.endpointUrl").getString()
                    )
                )
            } else {
                s3Client()
            }
        }
        single<ObjectStore> {
            S3Service(get(), environment.config.propertyOrNull("localstack.region")?.getString() ?: "us-east-1") // TODO
        }
    }

    install(Koin) {
        slf4jLogger()
        modules(appModule, dbModule, awsModule)
    }
}

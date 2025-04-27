package io

import aws.sdk.kotlin.services.s3.S3Client
import io.asset.*
import io.asset.store.AWSProperties
import io.asset.store.S3Service
import io.image.ImageProcessor
import io.image.ImageProperties
import io.image.PreProcessingProperties
import io.image.VipsImageProcessor
import io.image.store.ObjectStore
import io.ktor.server.application.*
import io.r2dbc.spi.ConnectionFactory
import org.jooq.DSLContext
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger

fun Application.configureKoin(
    connectionFactory: ConnectionFactory,
) {
    val appModule = module {
        single<PathAdapter> {
            PathAdapter()
        }
        single<AssetHandler> {
            AssetHandler(get(), get(), get())
        }
        single<MimeTypeDetector> {
            TikaMimeTypeDetector()
        }
        single<AssetService> {
            AssetServiceImpl(get(), get(), get())
        }
        single<ImageProcessor> {
            VipsImageProcessor(get())
        }
        single<ImageProperties> {
            ImageProperties(
                preProcessing = PreProcessingProperties(
                    enabled = environment.config.propertyOrNull("image.preprocessing.enabled")?.getString()?.toBoolean()
                        ?: false,
                    maxWidth = environment.config.propertyOrNull("image.preprocessing.maxWidth")?.getString()?.toInt()
                        ?: 1000,
                ),
            )
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
        val useMock = environment.config.propertyOrNull("aws.mock")?.getString().toBoolean()
        val region = environment.config.propertyOrNull("localstack.region")?.getString() ?: "us-east-1" // TODO
        single<S3Client> {
            if (useMock) {
                s3Client(
                    LocalstackProperties(
                        region = region,
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
            val port = environment.config.property("localstack.port").getString().toInt()
            val awsProperties = AWSProperties(
                host = if (useMock) "localhost:$port" else "s3-$region.amazonaws.com",
                region = region,
            )
            S3Service(get(), awsProperties)
        }
    }

    install(Koin) {
        slf4jLogger()
        modules(appModule, dbModule, awsModule)
    }
}

package io

import aws.sdk.kotlin.services.s3.S3Client
import io.asset.AssetHandler
import io.asset.AssetService
import io.asset.AssetServiceImpl
import io.asset.MimeTypeDetector
import io.asset.PathAdapter
import io.asset.TikaMimeTypeDetector
import io.asset.store.AWSProperties
import io.asset.store.S3Service
import io.image.ImageFormat
import io.image.ImageProcessor
import io.image.ImageProperties
import io.image.PreProcessingProperties
import io.image.VipsImageProcessor
import io.image.store.ObjectStore
import io.ktor.server.application.Application
import io.ktor.server.application.install
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
            ImageProperties.create(
                preProcessing = PreProcessingProperties.create(
                    enabled = environment.config.propertyOrNull("image.preprocessing.enabled")?.getString()?.toBoolean()
                        ?: false,
                    maxWidth = environment.config.propertyOrNull("image.preprocessing.maxWidth")?.getString()?.toInt(),
                    maxHeight = environment.config.propertyOrNull("image.preprocessing.maxHeight")?.getString()
                        ?.toInt(),
                    imageFormat = environment.config.propertyOrNull("image.preprocessing.imageFormat")?.getString()
                        ?.let {
                            ImageFormat.fromFormat(it)
                        }
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
                host = if (useMock) "localhost.localstack.cloud:$port" else "s3-$region.amazonaws.com",
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

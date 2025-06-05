package io.asset

import io.image.ImageFormat
import io.image.ImageProcessor
import io.image.ImageProperties
import io.image.PreProcessingProperties
import io.image.VipsImageProcessor
import io.ktor.server.application.Application
import org.koin.core.module.Module
import org.koin.dsl.module

fun Application.assetModule(): Module =
    module {
        single<AssetHandler> {
            AssetHandler(get(), get(), get(), get(), get())
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
                preProcessing =
                    PreProcessingProperties.create(
                        enabled =
                            environment.config.propertyOrNull("image.preprocessing.enabled")?.getString()
                                ?.toBoolean()
                                ?: false,
                        maxWidth =
                            environment.config.propertyOrNull("image.preprocessing.maxWidth")?.getString()
                                ?.toInt(),
                        maxHeight =
                            environment.config.propertyOrNull("image.preprocessing.maxHeight")?.getString()
                                ?.toInt(),
                        imageFormat =
                            environment.config.propertyOrNull("image.preprocessing.imageFormat")?.getString()
                                ?.let {
                                    ImageFormat.fromFormat(it)
                                },
                    ),
            )
        }
    }

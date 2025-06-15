package io.image

import io.ktor.server.application.Application
import io.tryGetConfig
import org.koin.core.module.Module
import org.koin.dsl.module

fun Application.imageModule(): Module =
    module {
        single<ImageProcessor> {
            VipsImageProcessor(get())
        }
        single<ImageProperties> {
            constructImageProperties(environment.config.tryGetConfig("image"))
        }
    }

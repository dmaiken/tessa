package io.asset

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
    }

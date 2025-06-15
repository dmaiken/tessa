package io.asset

import io.asset.repository.AssetRepository
import io.asset.repository.InMemoryAssetRepository
import io.asset.repository.PostgresAssetRepository
import io.r2dbc.spi.ConnectionFactory
import org.koin.core.module.Module
import org.koin.dsl.module

fun assetModule(connectionFactory: ConnectionFactory?): Module =
    module {
        single<AssetHandler> {
            AssetHandler(get(), get(), get(), get(), get(), get())
        }
        single<MimeTypeDetector> {
            TikaMimeTypeDetector()
        }

        single<AssetRepository> {
            connectionFactory?.let {
                PostgresAssetRepository(get(), get())
            } ?: InMemoryAssetRepository()
        }
    }

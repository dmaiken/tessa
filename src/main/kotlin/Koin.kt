package io

import io.image.ImageService
import io.image.ImageServiceImpl
import io.ktor.server.application.*
import io.r2dbc.spi.ConnectionFactory
import org.jooq.DSLContext
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger

fun Application.configureKoin(connectionFactory: ConnectionFactory) {
    val appModule = module {
        single<ImageService> {
            ImageServiceImpl(get())
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
    install(Koin) {
        slf4jLogger()
        modules(appModule, dbModule)
    }
}

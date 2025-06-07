package io

import io.asset.assetModule
import io.aws.awsModule
import io.image.imageModule
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.path.pathModule
import io.r2dbc.spi.ConnectionFactory
import org.jooq.DSLContext
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger

fun Application.configureKoin(connectionFactory: ConnectionFactory) {
    val dbModule =
        module {
            single<ConnectionFactory> {
                connectionFactory
            }
            single<DSLContext> {
                configureJOOQ(get())
            }
        }

    install(Koin) {
        slf4jLogger()
        modules(dbModule, assetModule(), awsModule(), pathModule(), imageModule())
    }
}

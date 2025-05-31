package io

import app.photofox.vipsffm.Vips
import io.ktor.server.application.Application
import io.ktor.util.logging.KtorSimpleLogger

private val logger = KtorSimpleLogger("io.Application")

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    logger.info("Initializing Vips")
    Vips.init()
    Runtime.getRuntime().addShutdownHook(
        object : Thread() {
            override fun run() {
                logger.info("Shutting down Vips")
                Vips.shutdown()
                logger.info("Vips has shutdown")
            }
        },
    )

    val connectionFactory = connectToPostgres()
    configureKoin(connectionFactory)
    configureContentNegotiation()
    configureRouting()
    configureStatusPages()
}

package io

import app.photofox.vipsffm.Vips
import io.ktor.server.application.*

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    Vips.init()
    Runtime.getRuntime().addShutdownHook(object : Thread() {
        override fun run() {
            Vips.shutdown()
        }
    })

    val connectionFactory = connectToPostgres()
    configureKoin(connectionFactory)
    configureContentNegotiation()
    configureRouting()
    configureStatusPages()
}

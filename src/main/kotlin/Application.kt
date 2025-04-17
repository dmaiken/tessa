package io

import io.ktor.server.application.*

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    val connectionFactory = connectToPostgres()
    configureKoin(connectionFactory)
    configureContentNegotiation()
    configureRouting()
    configureStatusPages()
}

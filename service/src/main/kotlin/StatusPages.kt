package io

import image.InvalidImageException
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.request.path
import io.ktor.server.response.respond
import io.ktor.util.logging.KtorSimpleLogger

private val logger = KtorSimpleLogger("io.StatusPages")

fun Application.configureStatusPages() =
    install(StatusPages) {
        exception<InvalidImageException> { call, cause ->
            call.respond(HttpStatusCode.BadRequest, cause.message ?: "")
        }
        exception<IllegalArgumentException> { call, cause ->
            logger.info("Returning ${HttpStatusCode.BadRequest} for ${call.request.path()}", cause)
            call.respond(HttpStatusCode.BadRequest, cause.message ?: "")
        }
    }

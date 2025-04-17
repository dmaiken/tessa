package io

import io.image.InvalidImageException
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*

fun Application.configureStatusPages() = install(StatusPages) {
    exception<InvalidImageException> { call, cause ->
        call.respond(HttpStatusCode.BadRequest, cause.message ?: "")
    }
}
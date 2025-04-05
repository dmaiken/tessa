package io

import io.image.ImageService
import io.image.StoreImageRequest
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject
import java.util.*

fun Application.configureRouting() {

    val imageService by inject<ImageService>()

    routing {

        post("/images") {
            val request = call.receive<StoreImageRequest>()
            val created = imageService.createImage(request).toResponse()
            call.respond(HttpStatusCode.Created, created)
        }

        get("/images/{id}") {
            val id = call.parameters["id"]
            imageService.fetchImage(UUID.fromString(id))?.let {
                call.respond(HttpStatusCode.OK, it.toResponse())
            } ?: call.respond(HttpStatusCode.NotFound)
        }
    }
}

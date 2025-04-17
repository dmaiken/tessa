package io

import io.image.ImageService
import io.image.StoreImageRequest
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.utils.io.*
import kotlinx.serialization.json.Json
import org.koin.ktor.ext.inject
import java.util.*

fun Application.configureRouting() {

    val imageService by inject<ImageService>()

    routing {
        post("/images") {
            var imageData: StoreImageRequest? = null
            var imageContent: ByteArray? = null
            val multipart = call.receiveMultipart()
            multipart.forEachPart { part ->
                when (part) {
                    is PartData.FormItem -> {
                        if (part.name == "metadata") {
                            imageData = Json.decodeFromString(part.value)
                        }
                    }

                    is PartData.FileItem -> {
                        imageContent = part.provider().toByteArray()
                    }

                    else -> {}
                }
                part.dispose()
            }
            if (imageData == null) {
                throw IllegalArgumentException("No image metadata supplied")
            }
            if (imageContent == null) {
                throw IllegalArgumentException("No image content supplied")
            }
            val created = imageService.storeImage(checkNotNull(imageData), checkNotNull(imageContent)).toResponse()
            call.respond(HttpStatusCode.Created, created)
        }

        get("/images/{id}") {
            val id = call.parameters["id"]
            imageService.fetchImage(UUID.fromString(id))?.let { image ->
                call.response.headers.append(HttpHeaders.Location, image.url)
                call.respond(HttpStatusCode.TemporaryRedirect)
            } ?: call.respond(HttpStatusCode.NotFound)
        }

        get("/images/{id}/info") {
            val id = call.parameters["id"]
            imageService.fetchImage(UUID.fromString(id))?.let {
                call.respond(HttpStatusCode.OK, it.toResponse())
            } ?: call.respond(HttpStatusCode.NotFound)
        }
    }
}

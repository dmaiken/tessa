package io

import io.asset.AssetService
import io.image.StoreAssetRequest
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

    val assetService by inject<AssetService>()

    routing {
        post("/assets") {
            var assetData: StoreAssetRequest? = null
            var assetContent: ByteArray? = null
            val multipart = call.receiveMultipart()
            multipart.forEachPart { part ->
                when (part) {
                    is PartData.FormItem -> {
                        if (part.name == "metadata") {
                            assetData = Json.decodeFromString(part.value)
                        }
                    }

                    is PartData.FileItem -> {
                        assetContent = part.provider().toByteArray()
                    }

                    else -> {}
                }
                part.dispose()
            }
            if (assetData == null) {
                throw IllegalArgumentException("No asset metadata supplied")
            }
            if (assetContent == null) {
                throw IllegalArgumentException("No asset content supplied")
            }
            val created = assetService.store(checkNotNull(assetData), checkNotNull(assetContent)).toResponse()
            call.respond(HttpStatusCode.Created, created)
        }

        get("/assets/{id}") {
            val id = call.parameters["id"]
            assetService.fetch(UUID.fromString(id))?.let { image ->
                call.response.headers.append(HttpHeaders.Location, image.url)
                call.respond(HttpStatusCode.TemporaryRedirect)
            } ?: call.respond(HttpStatusCode.NotFound)
        }

        get("/assets/{id}/info") {
            val id = call.parameters["id"]
            assetService.fetch(UUID.fromString(id))?.let {
                call.respond(HttpStatusCode.OK, it.toResponse())
            } ?: call.respond(HttpStatusCode.NotFound)
        }
    }
}

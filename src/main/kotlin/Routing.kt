package io

import asset.StoreAssetRequest
import io.asset.AssetHandler
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.plugins.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.logging.*
import io.ktor.utils.io.*
import kotlinx.serialization.json.Json
import org.koin.ktor.ext.inject

fun Application.configureRouting() {

    val assetHandler by inject<AssetHandler>()

    val logger = KtorSimpleLogger("io")

    routing {

        get("/assets/{...}") {
            val route = call.request.path()
            if (route.endsWith("/info")) {
                logger.info("Navigating to asset info with path: $route")
                assetHandler.fetchAssetInfoByPath(route)?.let {
                    logger.info("Found asset info: $it")
                    call.respond(HttpStatusCode.OK, it.toResponse())
                } ?: call.respond(HttpStatusCode.NotFound)
            } else {
                logger.info("Navigating to asset with path: $route")
                assetHandler.fetchAssetByPath(route)?.let { url ->
                    logger.info("Found asset with url: $url")
                    call.response.headers.append(HttpHeaders.Location, url)
                    call.respond(HttpStatusCode.TemporaryRedirect)
                } ?: call.respond(HttpStatusCode.NotFound)
            }
        }

//        get("/assets/{id}") {
//            val id = call.parameters["id"]
//            assetService.fetch(UUID.fromString(id))?.let { asset ->
//                call.response.headers.append(HttpHeaders.Location, asset.url)
//                call.respond(HttpStatusCode.TemporaryRedirect)
//            } ?: call.respond(HttpStatusCode.NotFound)
//        }

//        get("/assets/{id}/info") {
//            val id = call.parameters["id"]
//            assetService.fetch(UUID.fromString(id))?.let {
//                call.respond(HttpStatusCode.OK, it.toResponse())
//            } ?: call.respond(HttpStatusCode.NotFound)
//        }

        post("/assets") {
            createNewAsset(call, assetHandler)
        }

        post("/assets/{...}") {
            createNewAsset(call, assetHandler)
        }
    }
}

suspend fun createNewAsset(call: RoutingCall, assetHandler: AssetHandler) {
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
    val asset = assetHandler.storeNewAsset(
        request = checkNotNull(assetData),
        content = checkNotNull(assetContent),
        uriPath = call.request.path()
    )
    logger.info("Created asset under path: ${asset.locationPath}")

    call.response.headers.append(HttpHeaders.Location, "http//${call.request.origin.localAddress}${asset.locationPath}")
    call.respond(HttpStatusCode.Created, asset.asset.toResponse())
}

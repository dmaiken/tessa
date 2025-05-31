package io

import asset.StoreAssetRequest
import io.asset.AssetHandler
import io.asset.AssetReturnFormat
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.server.application.Application
import io.ktor.server.plugins.origin
import io.ktor.server.request.path
import io.ktor.server.request.receiveMultipart
import io.ktor.server.response.respond
import io.ktor.server.response.respondOutputStream
import io.ktor.server.routing.RoutingCall
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import io.ktor.util.logging.KtorSimpleLogger
import io.ktor.utils.io.toByteArray
import kotlinx.serialization.json.Json
import org.koin.ktor.ext.inject

private val logger = KtorSimpleLogger("io")

fun Application.configureRouting() {
    val assetHandler by inject<AssetHandler>()

    routing {
        get("/assets/{...}") {
            val route = call.request.path()
            val returnFormat = AssetReturnFormat.fromQueryParam(call.request.queryParameters["format"])
            val all = call.request.queryParameters["all"]?.toBoolean() ?: false
            val suppliedEntryId = getEntryId(call.request)

            if (returnFormat == AssetReturnFormat.METADATA && !all) {
                logger.info("Navigating to asset info with path: $route")
                assetHandler.fetchAssetInfoByPath(route, suppliedEntryId)?.let {
                    logger.info("Found asset info: $it with path: $route")
                    call.respond(HttpStatusCode.OK, it.toResponse())
                } ?: call.respond(HttpStatusCode.NotFound)
                return@get
            } else if (returnFormat == AssetReturnFormat.METADATA) {
                logger.info("Navigating to asset info of all assets with path: $route")
                assetHandler.fetchAssetInfoInPath(route).map {
                    it.toResponse()
                }.let {
                    logger.info("Found asset info for ${it.size} assets in path: $route")
                    call.respond(HttpStatusCode.OK, it)
                }
            } else if (returnFormat == AssetReturnFormat.REDIRECT) {
                logger.info("Navigating to asset with path: $route")
                assetHandler.fetchAssetByPath(route, suppliedEntryId)?.let { url ->
                    logger.info("Found asset with url: $url and route: $route")
                    call.response.headers.append(HttpHeaders.Location, url)
                    call.respond(HttpStatusCode.TemporaryRedirect)
                } ?: call.respond(HttpStatusCode.NotFound)
            } else {
                // Content
                logger.info("Navigating to asset content with path: $route")
                assetHandler.fetchAssetInfoByPath(route, suppliedEntryId)?.let { asset ->
                    logger.info("Found asset content with path: $route")
                    call.respondOutputStream(
                        contentType = ContentType.parse(asset.mimeType),
                        status = HttpStatusCode.OK,
                    ) {
                        assetHandler.fetchAssetContent(asset.bucket, asset.storeKey, this)
                    }
                } ?: call.respond(HttpStatusCode.NotFound)
            }
        }

        post("/assets") {
            createNewAsset(call, assetHandler)
        }

        post("/assets/{...}") {
            createNewAsset(call, assetHandler)
        }

        delete("/assets/{...}") {
            val suppliedEntryId = getEntryId(call.request)
            val suppliedOption = getPathModifierOption(call.request)
            if (suppliedOption != null && suppliedEntryId != null) {
                throw IllegalArgumentException("Both entryId and option cannot both be supplied")
            }
            if (suppliedOption != null) {
                assetHandler.deleteAssets(call.request.path(), suppliedOption)
            } else {
                assetHandler.deleteAsset(call.request.path(), suppliedEntryId)
            }

            call.respond(HttpStatusCode.NoContent)
        }
    }
}

suspend fun createNewAsset(
    call: RoutingCall,
    assetHandler: AssetHandler,
) {
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
    val asset =
        assetHandler.storeNewAsset(
            request = checkNotNull(assetData),
            content = checkNotNull(assetContent),
            uriPath = call.request.path(),
        )
    logger.info("Created asset under path: ${asset.locationPath}")

    call.response.headers.append(HttpHeaders.Location, "http//${call.request.origin.localAddress}${asset.locationPath}")
    call.respond(HttpStatusCode.Created, asset.asset.toResponse())
}

package io.asset

import asset.Asset
import asset.StoreAssetRequest
import io.asset.handler.StoreAssetDto
import io.image.InvalidImageException
import io.ktor.util.logging.*

class AssetHandler(
    private val mimeTypeDetector: MimeTypeDetector,
    private val assetService: AssetService,
    private val pathGenerator: PathAdapter
) {

    private val logger = KtorSimpleLogger("io.asset")

    suspend fun storeNewAsset(request: StoreAssetRequest, content: ByteArray, uriPath: String): AssetAndLocation {
        val treePath = pathGenerator.toTreePathFromUriPath(uriPath)
        val mimeType = deriveValidMimeType(content)
        val asset = assetService.store(
            StoreAssetDto(
                content = content,
                request = request,
                mimeType = mimeType,
                treePath = treePath,
            )
        )

        return AssetAndLocation(
            asset = asset,
            locationPath = uriPath
        )
    }

    suspend fun fetchAssetByPath(uriPath: String): String? {
        val treePath = pathGenerator.toTreePathFromUriPath(uriPath)
        logger.info("Fetching asset by path: $treePath")
        return assetService.fetchLatestByPath(treePath)?.url
    }

    suspend fun fetchAssetInfoByPath(uriPath: String): Asset? {
        val treePath = pathGenerator.toTreePathFromUriPath(uriPath.removeSuffix("/info"))
        logger.info("Fetching asset info by path: $treePath")
        return assetService.fetchLatestByPath(treePath)
    }

    private fun deriveValidMimeType(content: ByteArray): String {
        val mimeType = mimeTypeDetector.detect(content)
        if (!validate(mimeType)) {
            throw InvalidImageException("Not an image type")
        }
        return mimeType
    }

    private fun validate(mimeType: String): Boolean {
        return mimeType.startsWith("image/")
    }
}

data class AssetAndLocation(
    val asset: Asset,
    val locationPath: String
)
package io.asset

import asset.Asset
import asset.StoreAssetRequest
import asset.store.ObjectStore
import io.asset.handler.StoreAssetDto
import io.image.InvalidImageException
import io.ktor.util.logging.KtorSimpleLogger
import io.path.PathAdapter
import io.path.PathModifierOption
import io.path.configuration.PathConfiguration
import io.path.configuration.PathConfigurationService
import java.io.OutputStream

class AssetHandler(
    private val mimeTypeDetector: MimeTypeDetector,
    private val assetService: AssetService,
    private val pathGenerator: PathAdapter,
    private val objectStore: ObjectStore,
    private val pathConfigurationService: PathConfigurationService,
) {
    private val logger = KtorSimpleLogger("io.asset")

    suspend fun storeNewAsset(
        request: StoreAssetRequest,
        content: ByteArray,
        uriPath: String,
    ): AssetAndLocation {
        val mimeType = deriveValidMimeType(content)
        val pathConfiguration = validatePathConfiguration(uriPath, mimeType)
        val treePath = pathGenerator.toTreePathFromUriPath(uriPath)
        val asset =
            assetService.store(
                StoreAssetDto(
                    content = content,
                    request = request,
                    mimeType = mimeType,
                    treePath = treePath,
                ),
                pathConfiguration,
            )

        return AssetAndLocation(
            asset = asset,
            locationPath = uriPath,
        )
    }

    suspend fun fetchAssetByPath(
        uriPath: String,
        entryId: Long?,
    ): String? {
        return fetchAssetInfoByPath(uriPath, entryId)?.url
    }

    suspend fun fetchAssetInfoByPath(
        uriPath: String,
        entryId: Long?,
    ): Asset? {
        val treePath = pathGenerator.toTreePathFromUriPath(uriPath)
        logger.info("Fetching asset info by path: $treePath")
        return assetService.fetchByPath(treePath, entryId)
    }

    suspend fun fetchAssetInfoInPath(uriPath: String): List<Asset> {
        val treePath = pathGenerator.toTreePathFromUriPath(uriPath)
        logger.info("Fetching asset info in path: $treePath")
        return assetService.fetchAllByPath(treePath)
    }

    suspend fun fetchAssetContent(
        bucket: String,
        storeKey: String,
        stream: OutputStream,
    ): Long {
        return objectStore.fetch(bucket, storeKey, stream)
            .takeIf { it.found }?.contentLength
            ?: throw IllegalStateException("Asset not found in object store: $bucket/$storeKey")
    }

    suspend fun deleteAsset(
        uriPath: String,
        entryId: Long? = null,
    ) {
        val treePath = pathGenerator.toTreePathFromUriPath(uriPath)
        if (entryId == null) {
            logger.info("Deleting asset with path: $treePath")
        } else {
            logger.info("Deleting asset with path: $treePath and entry id: $entryId")
        }
        assetService.deleteAssetByPath(treePath, entryId)
    }

    suspend fun deleteAssets(
        uriPath: String,
        mode: PathModifierOption,
    ) {
        val treePath = pathGenerator.toTreePathFromUriPath(uriPath)
        if (mode == PathModifierOption.CHILDREN) {
            logger.info("Deleting assets at path: $treePath")
        } else {
            logger.info("Deleting assets at path: $treePath and all underneath it!")
        }
        assetService.deleteAssetsByPath(treePath, mode == PathModifierOption.RECURSIVE)
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

    private fun validatePathConfiguration(
        uriPath: String,
        mimeType: String,
    ): PathConfiguration? {
        return pathConfigurationService.fetch(uriPath)?.also { config ->
            config.allowedContentTypes?.contains(mimeType)?.let { allowedContentTypes ->
                if (!allowedContentTypes) {
                    throw IllegalArgumentException("Not an allowed content type: $mimeType for path: $uriPath")
                }
            }
        }
    }
}

data class AssetAndLocation(
    val asset: Asset,
    val locationPath: String,
)

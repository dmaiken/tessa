package io.asset.repository

import io.asset.AssetAndVariant
import io.asset.handler.StoreAssetDto
import io.image.ImageAttributes
import java.util.UUID

interface AssetRepository {
    suspend fun store(asset: StoreAssetDto): AssetAndVariant

    suspend fun fetchOriginalVariant(id: UUID): AssetAndVariant?

    suspend fun fetchByPath(
        treePath: String,
        entryId: Long?,
        imageAttributes: ImageAttributes?
    ): AssetAndVariant?

    suspend fun fetchAllByPath(treePath: String): List<AssetAndVariant>

    suspend fun deleteAssetByPath(
        treePath: String,
        entryId: Long? = null,
    )

    suspend fun deleteAssetsByPath(
        treePath: String,
        recursive: Boolean,
    )
}

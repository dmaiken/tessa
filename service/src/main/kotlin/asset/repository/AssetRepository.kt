package io.asset.repository

import asset.Asset
import io.asset.handler.StoreAssetDto
import java.util.UUID

interface AssetRepository {
    suspend fun store(asset: StoreAssetDto): Asset

    suspend fun fetch(id: UUID): Asset?

    suspend fun fetchByPath(
        treePath: String,
        entryId: Long?,
    ): Asset?

    suspend fun fetchAllByPath(treePath: String): List<Asset>

    suspend fun deleteAssetByPath(
        treePath: String,
        entryId: Long? = null,
    )

    suspend fun deleteAssetsByPath(
        treePath: String,
        recursive: Boolean,
    )
}

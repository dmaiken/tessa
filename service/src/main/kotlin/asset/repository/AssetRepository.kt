package io.asset.repository

import io.asset.AssetAndVariants
import io.asset.VariantBucketAndKey
import io.asset.handler.StoreAssetDto
import io.image.ImageAttributes

interface AssetRepository {
    suspend fun store(asset: StoreAssetDto): AssetAndVariants

    suspend fun fetchByPath(
        treePath: String,
        entryId: Long?,
        imageAttributes: ImageAttributes?,
    ): AssetAndVariants?

    suspend fun fetchAllByPath(treePath: String): List<AssetAndVariants>

    suspend fun deleteAssetByPath(
        treePath: String,
        entryId: Long? = null,
    ): List<VariantBucketAndKey>

    suspend fun deleteAssetsByPath(
        treePath: String,
        recursive: Boolean,
    ): List<VariantBucketAndKey>
}

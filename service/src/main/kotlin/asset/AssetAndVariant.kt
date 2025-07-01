package io.asset

import asset.Asset
import asset.AssetClass
import asset.AssetResponse
import asset.ImageAttributeResponse
import io.asset.variant.AssetVariant
import org.jooq.Record

data class AssetAndVariant(
    val asset: Asset,
    val variant: AssetVariant,
) {
    companion object Factory {
        fun from(record: Record): AssetAndVariant {
            return AssetAndVariant(
                asset = Asset.from(record),
                variant = AssetVariant.from(record),
            )
        }
    }

    fun toResponse(): AssetResponse =
        AssetResponse(
            id = asset.id,
            bucket = variant.objectStoreBucket,
            `class` = AssetClass.IMAGE,
            storeKey = variant.objectStoreKey,
            alt = asset.alt,
            entryId = asset.entryId,
            imageAttributes =
                ImageAttributeResponse(
                    height = variant.attributes.height,
                    width = variant.attributes.width,
                    mimeType = variant.attributes.mimeType,
                ),
            createdAt = asset.createdAt,
        )
}

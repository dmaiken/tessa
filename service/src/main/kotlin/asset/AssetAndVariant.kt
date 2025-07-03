package io.asset

import asset.Asset
import asset.AssetClass
import asset.AssetResponse
import asset.AssetVariantResponse
import asset.ImageAttributeResponse
import io.asset.variant.AssetVariant
import org.jooq.Record
import tessa.jooq.tables.records.AssetTreeRecord
import tessa.jooq.tables.records.AssetVariantRecord

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
            `class` = AssetClass.IMAGE,
            alt = asset.alt,
            entryId = asset.entryId,
            variants =
                listOf(
                    AssetVariantResponse(
                        bucket = variant.objectStoreBucket,
                        storeKey = variant.objectStoreKey,
                        imageAttributes =
                            ImageAttributeResponse(
                                height = variant.attributes.height,
                                width = variant.attributes.width,
                                mimeType = variant.attributes.mimeType,
                            ),
                    ),
                ),
            createdAt = asset.createdAt,
        )
}

data class AssetAndVariants(
    val asset: Asset,
    val variants: List<AssetVariant>,
) {
    companion object Factory {
        fun from(records: List<Record>): AssetAndVariants {
            if (records.isEmpty()) {
                throw IllegalArgumentException("No asset records")
            }
            val assetsToVariants = mutableMapOf<Asset, MutableList<AssetVariant>>()
            records.forEach { record ->
                val asset = Asset.from(record)
                if (assetsToVariants.containsKey(asset)) {
                    throw IllegalArgumentException("Multiple assets in record set")
                }
                val variants = assetsToVariants.computeIfAbsent(asset) { mutableListOf() }
                variants.add(AssetVariant.from(record))
            }
            val results =
                assetsToVariants.map { (asset, variants) ->
                    AssetAndVariants(
                        asset = asset,
                        variants = variants,
                    )
                }

            return results.first()
        }

        fun from(
            asset: AssetTreeRecord,
            variant: AssetVariantRecord,
        ): AssetAndVariants {
            return AssetAndVariants(
                asset = Asset.from(asset),
                variants = listOf(AssetVariant.from(variant)),
            )
        }
    }

    fun toResponse(): AssetResponse =
        AssetResponse(
            id = asset.id,
            `class` = AssetClass.IMAGE,
            alt = asset.alt,
            entryId = asset.entryId,
            variants =
                variants.map { variant ->
                    AssetVariantResponse(
                        bucket = variant.objectStoreBucket,
                        storeKey = variant.objectStoreKey,
                        imageAttributes =
                            ImageAttributeResponse(
                                height = variant.attributes.height,
                                width = variant.attributes.width,
                                mimeType = variant.attributes.mimeType,
                            ),
                    )
                },
            createdAt = asset.createdAt,
        )

    fun getOriginalVariant(): AssetVariant = variants.first { it.isOriginalVariant }
}

package asset.model

import asset.Asset
import asset.variant.AssetVariant
import org.jooq.Record
import tessa.jooq.tables.records.AssetTreeRecord
import tessa.jooq.tables.records.AssetVariantRecord

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

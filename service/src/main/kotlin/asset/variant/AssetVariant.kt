package io.asset.variant

import kotlinx.serialization.json.Json
import org.jooq.Record
import tessa.jooq.tables.AssetVariant.Companion.ASSET_VARIANT
import java.time.LocalDateTime

data class AssetVariant(
    val objectStoreBucket: String,
    val objectStoreKey: String,
    val attributes: ImageVariantAttributes,
    val isOriginalVariant: Boolean,
    val createdAt: LocalDateTime,
) {
    companion object Factory {
        fun from(record: Record): AssetVariant {
            return AssetVariant(
                objectStoreBucket = record.get(ASSET_VARIANT.OBJECT_STORE_BUCKET)!!,
                objectStoreKey = record.get(ASSET_VARIANT.OBJECT_STORE_KEY)!!,
                attributes = Json.decodeFromString(record.get(ASSET_VARIANT.ATTRIBUTES)!!.data()),
                isOriginalVariant = record.get(ASSET_VARIANT.ORIGINAL_VARIANT)!!,
                createdAt = record.get(ASSET_VARIANT.CREATED_AT)!!,
            )
        }
    }
}

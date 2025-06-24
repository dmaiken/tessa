package asset

import io.asset.repository.PostgresAssetRepository.AssetTreeAttributes.ALT
import io.asset.repository.PostgresAssetRepository.AssetTreeAttributes.ASSET_TREE_CREATED_AT
import io.asset.repository.PostgresAssetRepository.AssetTreeAttributes.ASSET_TREE_ID
import io.asset.repository.PostgresAssetRepository.AssetTreeAttributes.ENTRY_ID
import org.jooq.Record
import java.time.LocalDateTime
import java.util.UUID

data class Asset(
    val id: UUID = UUID.randomUUID(),
    val alt: String?,
    val entryId: Long,
    val createdAt: LocalDateTime = LocalDateTime.now(),
) {
    companion object {
        fun from(record: Record): Asset =
            Asset(
                id = record.getValue(ASSET_TREE_ID),
                alt = record.getValue(ALT),
                entryId = record.getValue(ENTRY_ID),
                createdAt = record.getValue(ASSET_TREE_CREATED_AT),
            )
    }
}

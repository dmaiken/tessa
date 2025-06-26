package asset

import org.jooq.Record
import tessa.jooq.tables.AssetTree.Companion.ASSET_TREE
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
                id = record.get(ASSET_TREE.ID)!!,
                alt = record.getValue(ASSET_TREE.ALT),
                entryId = record.getValue(ASSET_TREE.ENTRY_ID)!!,
                createdAt = record.getValue(ASSET_TREE.CREATED_AT)!!,
            )
    }
}

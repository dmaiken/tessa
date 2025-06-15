package io.asset.repository

import asset.Asset
import asset.store.ObjectStore
import io.asset.handler.StoreAssetDto
import io.ktor.util.logging.KtorSimpleLogger
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactive.collect
import org.jooq.DSLContext
import org.jooq.Record
import org.jooq.impl.DSL.condition
import org.jooq.impl.DSL.field
import org.jooq.impl.DSL.inline
import org.jooq.impl.DSL.max
import org.jooq.impl.DSL.name
import org.jooq.impl.DSL.table
import org.jooq.impl.SQLDataType
import org.jooq.kotlin.coroutines.transactionCoroutine
import org.jooq.postgres.extensions.bindings.LtreeBinding
import org.jooq.postgres.extensions.types.Ltree.ltree
import java.time.LocalDateTime
import java.util.UUID

class PostgresAssetRepository(
    private val dslContext: DSLContext,
    private val objectStore: ObjectStore,
) : AssetRepository {
    private val logger = KtorSimpleLogger(this::class.qualifiedName!!)

    override suspend fun store(asset: StoreAssetDto): Asset {
        val id = UUID.randomUUID()
        dslContext.transactionCoroutine { trx ->
            val entryId = getNextEntryId(trx.dsl(), asset.treePath)
            logger.info("Calculated entry_id: $entryId when storing new asset with path: ${asset.treePath}")
            trx.dsl().insertInto(table("asset_tree"))
                .set(field("id"), id)
                .set(field("path", SQLDataType.VARCHAR.asConvertedDataType(LtreeBinding())), ltree(asset.treePath))
                .set(field("height"), asset.imageAttributes.height)
                .set(field("width"), asset.imageAttributes.width)
                .set(field("bucket"), asset.persistResult.bucket)
                .set(field("store_key"), asset.persistResult.key)
                .set(field("url"), asset.persistResult.url)
                .set(field("mime_type"), asset.imageAttributes.mimeType)
                .set(field("alt"), asset.request.alt)
                .set(field("entry_id"), entryId)
                .set(field("created_at"), LocalDateTime.now())
                .awaitFirstOrNull()
        }

        return fetch(id) ?: throw IllegalStateException("Cannot find persisted image with id: $id")
    }

    override suspend fun fetch(id: UUID): Asset? {
        return dslContext.select()
            .from(table("asset_tree"))
            .where(field("id").eq(id))
            .awaitFirstOrNull()?.let {
                Asset.from(it)
            }
    }

    override suspend fun fetchByPath(
        treePath: String,
        entryId: Long?,
    ): Asset? {
        return fetch(dslContext, treePath, entryId)?.let {
            Asset.from(it)
        }
    }

    override suspend fun fetchAllByPath(treePath: String): List<Asset> {
        val assets = mutableListOf<Asset>()
        dslContext.select()
            .from(table("asset_tree"))
            .where(
                field(name("path"), String::class.java)
                    .cast(String::class.java)
                    .eq(treePath),
            )
            .orderBy(field("created_at").desc())
            .collect {
                assets.add(Asset.from(it))
            }
        return assets
    }

    override suspend fun deleteAssetByPath(
        treePath: String,
        entryId: Long?,
    ) {
        dslContext.transactionCoroutine { trx ->
            val asset = fetch(trx.dsl(), treePath, entryId)
            if (asset == null) {
                logger.info("Nothing to delete for path: $treePath")
                return@transactionCoroutine
            }

            logger.info("Deleting asset with path: $treePath and entry id: ${asset.get("entry_id")}")
            trx.dsl().deleteFrom(table("asset_tree"))
                .where(field("id").eq(asset.get("id")))
                .awaitFirstOrNull()

            objectStore.delete(
                bucket = asset.get("bucket", String::class.java),
                key = asset.get("store_key", String::class.java),
            )
        }
    }

    override suspend fun deleteAssetsByPath(
        treePath: String,
        recursive: Boolean,
    ) = coroutineScope {
        val deletedAssets =
            dslContext.transactionCoroutine { trx ->
                val assets =
                    if (recursive) {
                        fetchAllUnderPath(trx.dsl(), treePath).also {
                            logger.info("Found ${it.size} assets at path: $treePath for deletion")
                        }
                    } else {
                        fetchAllAtPath(trx.dsl(), treePath).also {
                            logger.info("Found ${it.size} assets descendents of path: $treePath for deletion")
                        }
                    }
                trx.dsl().deleteFrom(table("asset_tree"))
                    .where(
                        field("id")
                            .`in`(
                                *assets.map {
                                    it.get("id", UUID::class.java)
                                }.toTypedArray(),
                            ),
                    ).awaitFirstOrNull()
                assets
            }
        logger.info("Initiating deletes of ${deletedAssets.size} assets")
        val keysByBuckets = deletedAssets.groupBy { it.get("bucket", String::class.java) }
        keysByBuckets.forEach { (bucket, keys) ->
            objectStore.deleteAll(bucket, keys.map { it.get("store_key", String::class.java) })
        }
    }

    private suspend fun getNextEntryId(
        context: DSLContext,
        treePath: String,
    ): Long {
        val maxField = max(field("entry_id", Long::class.java)).`as`("max_entry")
        return context.select(maxField)
            .from(table("asset_tree"))
            .where(
                field(name("path"), String::class.java)
                    .cast(String::class.java)
                    .eq(treePath),
            )
            .awaitFirstOrNull()
            ?.get(maxField)
            ?.inc() ?: 0L
    }

    private suspend fun fetch(
        context: DSLContext,
        treePath: String,
        entryId: Long?,
    ): org.jooq.Record? {
        return context.select()
            .from(table("asset_tree"))
            .where(
                field(name("path"), String::class.java)
                    .cast(String::class.java)
                    .eq(treePath),
            ).let {
                if (entryId != null) {
                    it.and(field("entry_id").eq(entryId))
                } else {
                    it.orderBy(field("created_at").desc())
                }
            }
            .limit(1)
            .awaitFirstOrNull()
    }

    private suspend fun fetchAllAtPath(
        context: DSLContext,
        treePath: String,
    ): List<org.jooq.Record> {
        return context.select()
            .from(table("asset_tree"))
            .where(
                field(name("path"), String::class.java)
                    .cast(String::class.java)
                    .eq(treePath),
            ).asFlow()
            .toList()
    }

    private suspend fun fetchAllUnderPath(
        context: DSLContext,
        treePath: String,
    ): List<Record> {
        return context.select()
            .from(table("asset_tree"))
            .where(condition("path <@ {0}", inline(treePath)))
            .asFlow()
            .toList()
    }
}

package io.asset.repository

import asset.store.ObjectStore
import io.asset.AssetAndVariant
import io.asset.handler.StoreAssetDto
import io.asset.variant.VariantParameterGenerator
import io.image.ImageAttributes
import io.ktor.util.logging.KtorSimpleLogger
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactive.collect
import org.jooq.DSLContext
import org.jooq.JSONB
import org.jooq.Record
import org.jooq.impl.DSL.field
import org.jooq.impl.DSL.max
import org.jooq.impl.DSL.name
import org.jooq.kotlin.coroutines.transactionCoroutine
import org.jooq.postgres.extensions.types.Ltree
import org.jooq.postgres.extensions.types.Ltree.ltree
import tessa.jooq.tables.references.ASSET_TREE
import tessa.jooq.tables.references.ASSET_VARIANT
import java.time.LocalDateTime
import java.util.UUID

class PostgresAssetRepository(
    private val dslContext: DSLContext,
    private val objectStore: ObjectStore,
    private val variantParameterGenerator: VariantParameterGenerator,
) : AssetRepository {
    private val logger = KtorSimpleLogger(this::class.qualifiedName!!)

    override suspend fun store(asset: StoreAssetDto): AssetAndVariant {
        val assetId = UUID.randomUUID()
        val now = LocalDateTime.now()
        val variantAttributes = variantParameterGenerator.generateImageVariantAttributes(asset.imageAttributes)
        dslContext.transactionCoroutine { trx ->
            val entryId = getNextEntryId(trx.dsl(), asset.treePath)
            logger.info("Calculated entry_id: $entryId when storing new asset with path: ${asset.treePath}")
            trx.dsl().insertInto(ASSET_TREE)
                .set(ASSET_TREE.ID, assetId)
                .set(ASSET_TREE.PATH, ltree(asset.treePath))
                .set(ASSET_TREE.ALT, asset.request.alt)
                .set(ASSET_TREE.ENTRY_ID, entryId)
                .set(ASSET_TREE.CREATED_AT, now)
                .awaitFirstOrNull()

            trx.dsl().insertInto(ASSET_VARIANT)
                .set(ASSET_VARIANT.ID, UUID.randomUUID())
                .set(ASSET_VARIANT.ASSET_ID, assetId)
                .set(ASSET_VARIANT.OBJECT_STORE_BUCKET, asset.persistResult.bucket)
                .set(ASSET_VARIANT.OBJECT_STORE_KEY, asset.persistResult.key)
                .set(ASSET_VARIANT.ATTRIBUTES, JSONB.valueOf(variantAttributes))
                .set(ASSET_VARIANT.ORIGINAL_VARIANT, true)
                .set(ASSET_VARIANT.CREATED_AT, now)
                .awaitFirstOrNull()
        }

        return fetchOriginalVariant(assetId)
            ?: throw IllegalStateException("Cannot find persisted image with id: $assetId")
    }

    override suspend fun fetchOriginalVariant(id: UUID): AssetAndVariant? {
        return dslContext.select()
            .from(ASSET_TREE)
            .join(ASSET_VARIANT)
            .on(ASSET_VARIANT.ASSET_ID.eq(ASSET_TREE.ID))
            .where(ASSET_TREE.ID.eq(id))
            .and(ASSET_VARIANT.ORIGINAL_VARIANT.eq(true))
            .awaitFirstOrNull()?.let {
                AssetAndVariant.from(it)
            }
    }

    override suspend fun fetchByPath(
        treePath: String,
        entryId: Long?,
        imageAttributes: ImageAttributes?,
    ): AssetAndVariant? {
        return fetch(dslContext, treePath, entryId)?.let {
            AssetAndVariant.from(it)
        }
    }

    override suspend fun fetchAllByPath(treePath: String): List<AssetAndVariant> {
        val assetAndVariants = mutableListOf<AssetAndVariant>()
        dslContext.select()
            .from(ASSET_TREE)
            .where(ASSET_TREE.PATH.eq(Ltree.valueOf(treePath)))
            .orderBy(ASSET_TREE.CREATED_AT.desc())
            .collect {
                assetAndVariants.add(AssetAndVariant.from(it))
            }
        return assetAndVariants
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

            logger.info("Deleting asset with path: $treePath and entryId: ${asset.get(ASSET_TREE.ENTRY_ID)}")
            trx.dsl().deleteFrom(ASSET_TREE)
                .where(ASSET_TREE.ID.eq(asset.get(ASSET_TREE.ID)))
                .awaitFirstOrNull()

            objectStore.delete(
                bucket = asset.get(ASSET_VARIANT.OBJECT_STORE_BUCKET)!!,
                key = asset.get(ASSET_VARIANT.OBJECT_STORE_KEY)!!,
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
                trx.dsl().deleteFrom(ASSET_TREE)
                    .where(
                        ASSET_TREE.ID
                            .`in`(
                                *assets.map {
                                    it.get(ASSET_TREE.ID)
                                }.toTypedArray(),
                            ),
                    ).awaitFirstOrNull()
                assets
            }
        logger.info("Initiating deletes of ${deletedAssets.size} assets")
        val keysByBuckets = deletedAssets.groupBy { it.get(ASSET_VARIANT.OBJECT_STORE_BUCKET, String::class.java) }
        keysByBuckets.forEach { (bucket, keys) ->
            objectStore.deleteAll(bucket, keys.map { it.get(ASSET_VARIANT.OBJECT_STORE_KEY, String::class.java) })
        }
    }

    private suspend fun getNextEntryId(
        context: DSLContext,
        treePath: String,
    ): Long {
        val maxField = max(ASSET_TREE.ENTRY_ID).`as`("max_entry")
        return context.select(maxField)
            .from(ASSET_TREE)
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
    ): Record? {
        return context.select()
            .from(ASSET_TREE)
            .where(
                field(name("path"), String::class.java)
                    .cast(String::class.java)
                    .eq(treePath),
            ).let {
                if (entryId != null) {
                    it.and(ASSET_TREE.ENTRY_ID.eq(entryId))
                } else {
                    it.orderBy(ASSET_TREE.CREATED_AT.desc())
                }
            }
            .limit(1)
            .awaitFirstOrNull()
    }

    private suspend fun fetchAllAtPath(
        context: DSLContext,
        treePath: String,
    ): List<Record> {
        return context.select()
            .from(ASSET_TREE)
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
            .from(ASSET_TREE)
            .join(ASSET_VARIANT)
            .on(ASSET_VARIANT.ASSET_ID.eq(ASSET_TREE.ID))
            .where(ASSET_TREE.PATH.contains(Ltree.valueOf(treePath)))
//            .where(condition("$PATH <@ {0}", inline(treePath)))
            .asFlow()
            .toList()
    }
}

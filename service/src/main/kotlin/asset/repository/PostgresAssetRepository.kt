package io.asset.repository

import asset.store.ObjectStore
import io.asset.AssetAndVariant
import io.asset.handler.StoreAssetDto
import io.asset.repository.PostgresAssetRepository.AssetTreeAttributes.ALT
import io.asset.repository.PostgresAssetRepository.AssetTreeAttributes.ASSET_TREE_CREATED_AT
import io.asset.repository.PostgresAssetRepository.AssetTreeAttributes.ASSET_TREE_ID
import io.asset.repository.PostgresAssetRepository.AssetTreeAttributes.ASSET_TREE_TABLE
import io.asset.repository.PostgresAssetRepository.AssetTreeAttributes.ASSET_TREE_TABLE_ALIAS
import io.asset.repository.PostgresAssetRepository.AssetTreeAttributes.ENTRY_ID
import io.asset.repository.PostgresAssetRepository.AssetTreeAttributes.PATH
import io.asset.repository.PostgresAssetRepository.AssetVariantAttributes.ASSET_ID
import io.asset.repository.PostgresAssetRepository.AssetVariantAttributes.ASSET_VARIANT_CREATED_AT
import io.asset.repository.PostgresAssetRepository.AssetVariantAttributes.ASSET_VARIANT_ID
import io.asset.repository.PostgresAssetRepository.AssetVariantAttributes.ASSET_VARIANT_TABLE
import io.asset.repository.PostgresAssetRepository.AssetVariantAttributes.ASSET_VARIANT_TABLE_ALIAS
import io.asset.repository.PostgresAssetRepository.AssetVariantAttributes.ATTRIBUTES
import io.asset.repository.PostgresAssetRepository.AssetVariantAttributes.OBJECT_STORE_BUCKET
import io.asset.repository.PostgresAssetRepository.AssetVariantAttributes.OBJECT_STORE_KEY
import io.asset.repository.PostgresAssetRepository.AssetVariantAttributes.ORIGINAL_VARIANT
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
    private val variantParameterGenerator: VariantParameterGenerator
) : AssetRepository {

    object AssetTreeAttributes {
        const val ASSET_TREE_TABLE_ALIAS = "at"
        val ASSET_TREE_TABLE = table("asset_tree").`as`(ASSET_TREE_TABLE_ALIAS)
        val ASSET_TREE_ID = field("id", UUID::class.java)
        val ENTRY_ID = field("entry_id", Long::class.java)
        val PATH = field("path", SQLDataType.VARCHAR.asConvertedDataType(LtreeBinding()))
        val ALT = field("alt", String::class.java)
        val ASSET_TREE_CREATED_AT = field("created_at", LocalDateTime::class.java)
    }

    object AssetVariantAttributes {
        const val ASSET_VARIANT_TABLE_ALIAS = "av"
        val ASSET_VARIANT_TABLE = table("asset_variant").`as`(ASSET_VARIANT_TABLE_ALIAS)
        val ASSET_VARIANT_ID = field("id", UUID::class.java)
        val ASSET_ID = field("asset_id", UUID::class.java)
        val OBJECT_STORE_BUCKET = field("object_store_bucket", String::class.java)
        val OBJECT_STORE_KEY = field("object_store_key", String::class.java)
        val ATTRIBUTES = field("attributes", SQLDataType.JSONB)
        val ORIGINAL_VARIANT = field("original_variant", Boolean::class.java)
        val ASSET_VARIANT_CREATED_AT = field("created_at", LocalDateTime::class.java)
    }

    private val logger = KtorSimpleLogger(this::class.qualifiedName!!)

    override suspend fun store(asset: StoreAssetDto): AssetAndVariant {
        val assetId = UUID.randomUUID()
        val now = LocalDateTime.now()
        val variantAttributes = variantParameterGenerator.generateImageVariantAttributes(asset.imageAttributes)
        dslContext.transactionCoroutine { trx ->
            val entryId = getNextEntryId(trx.dsl(), asset.treePath)
            logger.info("Calculated $ENTRY_ID: $entryId when storing new asset with path: ${asset.treePath}")
            trx.dsl().insertInto(ASSET_TREE_TABLE)
                .set(ASSET_TREE_ID, assetId)
                .set(PATH, ltree(asset.treePath))
                .set(ALT, asset.request.alt)
                .set(ENTRY_ID, entryId)
                .set(ASSET_TREE_CREATED_AT, now)
                .awaitFirstOrNull()

            trx.dsl().insertInto(ASSET_VARIANT_TABLE)
                .set(ASSET_VARIANT_ID, UUID.randomUUID())
                .set(ASSET_ID, assetId)
                .set(OBJECT_STORE_BUCKET, asset.persistResult.bucket)
                .set(OBJECT_STORE_KEY, asset.persistResult.key)
                .set(ATTRIBUTES, JSONB.valueOf(variantAttributes))
                .set(ORIGINAL_VARIANT, true)
                .set(ASSET_VARIANT_CREATED_AT, now)
                .awaitFirstOrNull()
        }

        return fetchOriginalVariant(assetId)
            ?: throw IllegalStateException("Cannot find persisted image with id: $assetId")
    }

    override suspend fun fetchOriginalVariant(id: UUID): AssetAndVariant? {
        return dslContext.select()
            .from(ASSET_TREE_TABLE)
            .join(ASSET_VARIANT_TABLE)
            .on(field("$ASSET_VARIANT_TABLE_ALIAS.asset_id").eq(field("$ASSET_TREE_TABLE_ALIAS.id")))
            .where(ASSET_TREE_ID.eq(id))
            .and(ORIGINAL_VARIANT.eq(true))
            .awaitFirstOrNull()?.let {
                AssetAndVariant.from(it)
            }
    }

    override suspend fun fetchByPath(
        treePath: String,
        entryId: Long?,
        imageAttributes: ImageAttributes?
    ): AssetAndVariant? {
        return fetch(dslContext, treePath, entryId)?.let {
            AssetAndVariant.from(it)
        }
    }

    override suspend fun fetchAllByPath(treePath: String): List<AssetAndVariant> {
        val assetAndVariants = mutableListOf<AssetAndVariant>()
        dslContext.select()
            .from(ASSET_TREE_TABLE)
            .where(
                field(name("path"), String::class.java)
                    .cast(String::class.java)
                    .eq(treePath),
            )
            .orderBy(ASSET_TREE_CREATED_AT.desc())
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

            logger.info("Deleting asset with path: $treePath and $ENTRY_ID: ${asset.get(ENTRY_ID)}")
            trx.dsl().deleteFrom(ASSET_TREE_TABLE)
                .where(ASSET_TREE_ID.eq(asset.get(ASSET_TREE_ID)))
                .awaitFirstOrNull()

            objectStore.delete(
                bucket = asset.get(OBJECT_STORE_BUCKET, String::class.java),
                key = asset.get(OBJECT_STORE_KEY, String::class.java),
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
                trx.dsl().deleteFrom(ASSET_TREE_TABLE)
                    .where(
                        ASSET_TREE_ID
                            .`in`(
                                *assets.map {
                                    it.get(ASSET_TREE_ID)
                                }.toTypedArray(),
                            ),
                    ).awaitFirstOrNull()
                assets
            }
        logger.info("Initiating deletes of ${deletedAssets.size} assets")
        val keysByBuckets = deletedAssets.groupBy { it.get(OBJECT_STORE_BUCKET, String::class.java) }
        keysByBuckets.forEach { (bucket, keys) ->
            objectStore.deleteAll(bucket, keys.map { it.get(OBJECT_STORE_KEY, String::class.java) })
        }
    }

    private suspend fun getNextEntryId(
        context: DSLContext,
        treePath: String,
    ): Long {
        val maxField = max(ENTRY_ID).`as`("max_entry")
        return context.select(maxField)
            .from(ASSET_TREE_TABLE)
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
            .from(ASSET_TREE_TABLE)
            .where(
                field(name("path"), String::class.java)
                    .cast(String::class.java)
                    .eq(treePath),
            ).let {
                if (entryId != null) {
                    it.and(ENTRY_ID.eq(entryId))
                } else {
                    it.orderBy(ASSET_TREE_CREATED_AT.desc())
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
            .from(ASSET_TREE_TABLE)
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
            .from(ASSET_TREE_TABLE)
            .join(ASSET_VARIANT_TABLE)
            .on(field("$ASSET_VARIANT_TABLE_ALIAS.asset_id").eq("$ASSET_VARIANT_TABLE_ALIAS.id"))
            .where(condition("$PATH <@ {0}", inline(treePath)))
            .asFlow()
            .toList()
    }
}

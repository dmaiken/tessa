package asset.repository

import asset.handler.StoreAssetDto
import asset.model.AssetAndVariants
import asset.model.VariantBucketAndKey
import asset.variant.VariantParameterGenerator
import io.image.ImageAttributes
import io.ktor.util.logging.KtorSimpleLogger
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.jooq.DSLContext
import org.jooq.JSONB
import org.jooq.Record
import org.jooq.impl.DSL.max
import org.jooq.impl.DSL.noCondition
import org.jooq.kotlin.coroutines.transactionCoroutine
import org.jooq.postgres.extensions.types.Ltree
import org.jooq.postgres.extensions.types.Ltree.ltree
import tessa.jooq.tables.references.ASSET_TREE
import tessa.jooq.tables.references.ASSET_VARIANT
import java.time.LocalDateTime
import java.util.UUID

class PostgresAssetRepository(
    private val dslContext: DSLContext,
    private val variantParameterGenerator: VariantParameterGenerator,
) : AssetRepository {
    private val logger = KtorSimpleLogger(this::class.qualifiedName!!)

    override suspend fun store(asset: StoreAssetDto): AssetAndVariants {
        val assetId = UUID.randomUUID()
        val now = LocalDateTime.now()
        val variantAttributes = variantParameterGenerator.generateImageVariantAttributes(asset.imageAttributes)
        return dslContext.transactionCoroutine { trx ->
            val entryId = getNextEntryId(trx.dsl(), asset.treePath)
            logger.info("Calculated entry_id: $entryId when storing new asset with path: ${asset.treePath}")
            val persistedAsset =
                trx.dsl().insertInto(ASSET_TREE)
                    .set(ASSET_TREE.ID, assetId)
                    .set(ASSET_TREE.PATH, ltree(asset.treePath))
                    .set(ASSET_TREE.ALT, asset.request.alt)
                    .set(ASSET_TREE.ENTRY_ID, entryId)
                    .set(ASSET_TREE.CREATED_AT, now)
                    .returning()
                    .awaitFirst()

            val persistedVariant =
                trx.dsl().insertInto(ASSET_VARIANT)
                    .set(ASSET_VARIANT.ID, UUID.randomUUID())
                    .set(ASSET_VARIANT.ASSET_ID, assetId)
                    .set(ASSET_VARIANT.OBJECT_STORE_BUCKET, asset.persistResult.bucket)
                    .set(ASSET_VARIANT.OBJECT_STORE_KEY, asset.persistResult.key)
                    .set(ASSET_VARIANT.ATTRIBUTES, JSONB.valueOf(variantAttributes))
                    .set(ASSET_VARIANT.ORIGINAL_VARIANT, true)
                    .set(ASSET_VARIANT.CREATED_AT, now)
                    .returning()
                    .awaitFirst()

            AssetAndVariants.from(persistedAsset, persistedVariant)
        }
    }

    override suspend fun fetchByPath(
        treePath: String,
        entryId: Long?,
        imageAttributes: ImageAttributes?,
    ): AssetAndVariants? {
        return fetchWithVariant(dslContext, treePath, entryId, imageAttributes)?.let {
            AssetAndVariants.from(listOf(it))
        }
    }

    override suspend fun fetchAllByPath(treePath: String): List<AssetAndVariants> {
        return dslContext.select()
            .from(ASSET_TREE)
            .join(ASSET_VARIANT)
            .on(ASSET_VARIANT.ASSET_ID.eq(ASSET_TREE.ID))
            .where(ASSET_TREE.PATH.eq(Ltree.valueOf(treePath)))
            .orderBy(ASSET_TREE.ENTRY_ID.desc(), ASSET_VARIANT.CREATED_AT.desc())
            .asFlow()
            .toList()
            .groupBy { it.get(ASSET_TREE.ID) }
            .values
            .map { AssetAndVariants.from(it) }
    }

    override suspend fun deleteAssetByPath(
        treePath: String,
        entryId: Long?,
    ): List<VariantBucketAndKey> {
        val objectStoreInformation =
            dslContext.transactionCoroutine { trx ->
                val variantObjectStoreInformation =
                    trx.dsl()
                        .select(ASSET_VARIANT.OBJECT_STORE_BUCKET, ASSET_VARIANT.OBJECT_STORE_KEY)
                        .from(ASSET_TREE)
                        .join(ASSET_VARIANT).on(ASSET_TREE.ID.eq(ASSET_VARIANT.ASSET_ID))
                        .where(ASSET_TREE.PATH.eq(Ltree.valueOf(treePath)))
                        .let {
                            if (entryId != null) {
                                it.and(ASSET_TREE.ENTRY_ID.eq(entryId))
                            } else {
                                it.orderBy(ASSET_TREE.CREATED_AT.desc())
                            }
                        }
                        .asFlow()
                        .map {
                            VariantBucketAndKey(
                                bucket = it.getNonNull(ASSET_VARIANT.OBJECT_STORE_BUCKET),
                                key = it.getNonNull(ASSET_VARIANT.OBJECT_STORE_KEY),
                            )
                        }.toList()

                trx.dsl()
                    .deleteFrom(ASSET_TREE)
                    .where(ASSET_TREE.PATH.eq(Ltree.valueOf(treePath)))
                    .let {
                        if (entryId != null) {
                            it.and(ASSET_TREE.ENTRY_ID.eq(entryId))
                        } else {
                            it.and(
                                ASSET_TREE.ENTRY_ID.eq(
                                    trx.dsl().select(ASSET_TREE.ENTRY_ID)
                                        .from(ASSET_TREE)
                                        .where(ASSET_TREE.PATH.eq(Ltree.valueOf(treePath)))
                                        .orderBy(ASSET_TREE.CREATED_AT.desc())
                                        .limit(1),
                                ),
                            )
                        }
                    }.awaitFirstOrNull()

                variantObjectStoreInformation
            }
        return objectStoreInformation
    }

    override suspend fun deleteAssetsByPath(
        treePath: String,
        recursive: Boolean,
    ) = coroutineScope {
        val deletedAssets =
            dslContext.transactionCoroutine { trx ->
                val objectStoreInformation =
                    trx.dsl()
                        .select(ASSET_VARIANT.OBJECT_STORE_BUCKET, ASSET_VARIANT.OBJECT_STORE_KEY)
                        .from(ASSET_TREE)
                        .join(ASSET_VARIANT).on(ASSET_TREE.ID.eq(ASSET_VARIANT.ASSET_ID))
                        .let {
                            if (recursive) {
                                it.where(ASSET_TREE.PATH.contains(Ltree.valueOf(treePath)))
                            } else {
                                it.where(ASSET_TREE.PATH.eq(Ltree.valueOf(treePath)))
                            }
                        }
                        .asFlow()
                        .map {
                            VariantBucketAndKey(
                                bucket = it.getNonNull(ASSET_VARIANT.OBJECT_STORE_BUCKET),
                                key = it.getNonNull(ASSET_VARIANT.OBJECT_STORE_KEY),
                            )
                        }
                        .toList()

                trx.dsl().deleteFrom(ASSET_TREE)
                    .let {
                        if (recursive) {
                            it.where(ASSET_TREE.PATH.contains(Ltree.valueOf(treePath)))
                        } else {
                            it.where(ASSET_TREE.PATH.eq(Ltree.valueOf(treePath)))
                        }
                    }
                    .awaitFirstOrNull()
                objectStoreInformation
            }

        deletedAssets
    }

    private suspend fun getNextEntryId(
        context: DSLContext,
        treePath: String,
    ): Long {
        val maxField = max(ASSET_TREE.ENTRY_ID).`as`("max_entry")
        return context.select(maxField)
            .from(ASSET_TREE)
            .where(ASSET_TREE.PATH.eq(Ltree.valueOf(treePath)))
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
                ASSET_TREE.PATH.eq(Ltree.valueOf(treePath)),
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

    private suspend fun fetchWithVariant(
        context: DSLContext,
        treePath: String,
        entryId: Long?,
        imageAttributes: ImageAttributes?,
    ): Record? {
        val entryIdCondition =
            entryId?.let {
                ASSET_TREE.ENTRY_ID.eq(entryId)
            } ?: noCondition()
        val orderConditions =
            entryId?.let {
                arrayOf(ASSET_TREE.ENTRY_ID.desc(), ASSET_VARIANT.CREATED_AT.desc())
            } ?: arrayOf(ASSET_VARIANT.CREATED_AT.desc())

        return context.select()
            .from(ASSET_TREE)
            .join(ASSET_VARIANT).on(ASSET_VARIANT.ASSET_ID.eq(ASSET_TREE.ID))
            .where(
                ASSET_TREE.PATH.eq(Ltree.valueOf(treePath)),
            ).and(entryIdCondition)
            .orderBy(*orderConditions)
            .limit(1)
            .awaitFirstOrNull()
    }

    private suspend fun fetchAllAtPath(
        context: DSLContext,
        treePath: String,
    ): List<Record> {
        return context.select()
            .from(ASSET_TREE)
            .where(ASSET_TREE.PATH.eq(Ltree.valueOf(treePath)))
            .asFlow()
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
            .asFlow()
            .toList()
    }
}

package io.asset

import asset.Asset
import io.asset.handler.StoreAssetDto
import io.image.ImageProcessor
import io.image.store.ObjectStore
import io.ktor.util.logging.*
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactive.collect
import org.jooq.DSLContext
import org.jooq.impl.DSL.*
import org.jooq.impl.SQLDataType
import org.jooq.kotlin.coroutines.transactionCoroutine
import org.jooq.postgres.extensions.bindings.LtreeBinding
import org.jooq.postgres.extensions.types.Ltree.ltree
import java.time.LocalDateTime
import java.util.*

interface AssetService {
    suspend fun store(asset: StoreAssetDto): Asset
    suspend fun fetch(id: UUID): Asset?
    suspend fun fetchLatestByPath(treePath: String): Asset?
    suspend fun fetchAllByPath(treePath: String): List<Asset>
}

class AssetServiceImpl(
    private val dslContext: DSLContext,
    private val objectStore: ObjectStore,
    private val imageProcessor: ImageProcessor,
) : AssetService {

    val logger = KtorSimpleLogger(this::class.qualifiedName!!)

    override suspend fun store(asset: StoreAssetDto): Asset {
        val preProcessed = imageProcessor.preprocess(asset.content, asset.mimeType)
        val persistResult = objectStore.persist(asset.request, preProcessed.image)

        val id = UUID.randomUUID()
        dslContext.transactionCoroutine { trx ->
            val entryId = getNextEntryId(trx.dsl(), asset.treePath)
            logger.info("Calculated entry_id: $entryId when storing new asset with path: ${asset.treePath}")
            trx.dsl().insertInto(table("asset_tree"))
                .set(field("id"), id)
                .set(field("path", SQLDataType.VARCHAR.asConvertedDataType(LtreeBinding())), ltree(asset.treePath))
                .set(field("height"), preProcessed.attributes.height)
                .set(field("width"), preProcessed.attributes.width)
                .set(field("bucket"), persistResult.bucket)
                .set(field("store_key"), persistResult.key)
                .set(field("url"), persistResult.url)
                .set(field("mime_type"), asset.mimeType)
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

    override suspend fun fetchLatestByPath(treePath: String): Asset? {
        return dslContext.select()
            .from(table("asset_tree"))
            .where(
                field(name("path"), String::class.java)
                    .cast(String::class.java)
                    .eq(treePath)
            )
            .orderBy(field("created_at").desc())
            .limit(1)
            .awaitFirstOrNull()?.let {
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
                    .eq(treePath)
            )
            .orderBy(field("created_at").desc())
            .collect {
                assets.add(Asset.from(it))
            }
        return assets
    }

    private suspend fun getNextEntryId(context: DSLContext, treePath: String): Int {
        val maxField = max(field("entry_id", Int::class.java)).`as`("max_entry")
        return context.select(maxField)
            .from(table("asset_tree"))
            .where(
                field(name("path"), String::class.java)
                    .cast(String::class.java)
                    .eq(treePath)
            )
            .awaitFirstOrNull()
            ?.get(maxField)
            ?.inc() ?: 0
    }
}
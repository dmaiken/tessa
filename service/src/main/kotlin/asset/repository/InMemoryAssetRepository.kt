package io.asset.repository

import asset.Asset
import io.asset.handler.StoreAssetDto
import io.ktor.util.logging.KtorSimpleLogger
import java.time.LocalDateTime
import java.util.UUID

class InMemoryAssetRepository : AssetRepository {
    private val logger = KtorSimpleLogger(this::class.qualifiedName!!)
    private val store = mutableMapOf<String, MutableList<Asset>>()
    private val idReference = mutableMapOf<UUID, Asset>()
    private var currentEntryId: Long = 0

    override suspend fun store(asset: StoreAssetDto): Asset {
        val list = store.computeIfAbsent(asset.treePath) { mutableListOf() }

        logger.info("Persisting asset at path: ${asset.treePath} and entryId: $currentEntryId")
        return Asset(
            id = UUID.randomUUID(),
            bucket = asset.persistResult.bucket,
            storeKey = asset.persistResult.key,
            url = asset.persistResult.url,
            mimeType = asset.imageAttributes.mimeType,
            alt = asset.request.alt,
            height = asset.imageAttributes.height,
            width = asset.imageAttributes.width,
            entryId = currentEntryId,
            createdAt = LocalDateTime.now(),
        ).also {
            list.add(it)
            idReference.put(it.id, it)
            currentEntryId++
        }
    }

    override suspend fun fetch(id: UUID): Asset? {
        return idReference[id]
    }

    override suspend fun fetchByPath(
        treePath: String,
        entryId: Long?,
    ): Asset? {
        return store[treePath]?.let { assets ->
            (entryId ?: assets.maxByOrNull { it.createdAt }?.entryId)?.let { resolvedEntryId ->
                assets.firstOrNull { it.entryId == resolvedEntryId }
            }
        }
    }

    override suspend fun fetchAllByPath(treePath: String): List<Asset> {
        return store[treePath]?.toList()?.sortedBy { it.entryId }?.reversed() ?: emptyList()
    }

    override suspend fun deleteAssetByPath(
        treePath: String,
        entryId: Long?,
    ) {
        logger.info("Deleting asset at path: $treePath and entryId: ${entryId ?: "not specified"}")
        val asset = fetchByPath(treePath, entryId)
        asset?.let {
            idReference.remove(it.id)
        }
        store[treePath]?.let { assets ->
            (entryId ?: assets.maxByOrNull { it.createdAt }?.entryId)?.let { resolvedEntryId ->
                assets.removeIf { it.entryId == resolvedEntryId }
            }
        }
    }

    override suspend fun deleteAssetsByPath(
        treePath: String,
        recursive: Boolean,
    ) {
        if (recursive) {
            logger.info("Deleting assets (recursively) at path: $treePath")
            store.keys.filter { it.startsWith(treePath) }.forEach { path ->
                store[path]?.map { it.id }?.forEach {
                    idReference.remove(it)
                }
                store.remove(path)
            }
        } else {
            logger.info("Deleting assets at path: $treePath")
            store[treePath]?.map { it.id }?.forEach {
                idReference.remove(it)
            }
            store.remove(treePath)
        }
    }
}

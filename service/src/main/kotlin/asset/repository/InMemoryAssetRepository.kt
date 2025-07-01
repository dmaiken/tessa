package io.asset.repository

import asset.Asset
import io.asset.AssetAndVariant
import io.asset.handler.StoreAssetDto
import io.asset.variant.AssetVariant
import io.asset.variant.ImageVariantAttributes
import io.asset.variant.VariantParameterGenerator
import io.image.ImageAttributes
import io.ktor.util.logging.KtorSimpleLogger
import java.time.LocalDateTime
import java.util.UUID

class InMemoryAssetRepository(
    private val variantParameterGenerator: VariantParameterGenerator,
) : AssetRepository {
    private val logger = KtorSimpleLogger(this::class.qualifiedName!!)
    private val store = mutableMapOf<String, MutableList<AssetAndVariants>>()
    private val idReference = mutableMapOf<UUID, AssetAndVariant>()
    private var currentEntryId: Long = 0

    override suspend fun store(asset: StoreAssetDto): AssetAndVariant {
        logger.info("Persisting asset at path: ${asset.treePath} and entryId: $currentEntryId")
        val assetAndVariant =
            AssetAndVariant(
                asset =
                    Asset(
                        id = UUID.randomUUID(),
                        alt = asset.request.alt,
                        entryId = currentEntryId,
                        createdAt = LocalDateTime.now(),
                    ),
                variant =
                    AssetVariant(
                        objectStoreBucket = asset.persistResult.bucket,
                        objectStoreKey = asset.persistResult.key,
                        attributes =
                            ImageVariantAttributes(
                                height = asset.imageAttributes.height,
                                width = asset.imageAttributes.width,
                                mimeType = asset.imageAttributes.mimeType,
                            ),
                        isOriginalVariant = true,
                        createdAt = LocalDateTime.now(),
                    ),
            )
        return assetAndVariant.also {
            val originalVariantAttributeKey =
                variantParameterGenerator.generateImageVariantAttributes(asset.imageAttributes)
            store.computeIfAbsent(asset.treePath) { mutableListOf() }.add(
                AssetAndVariants(
                    asset = it.asset,
                    originalVariantAttributeKey = originalVariantAttributeKey,
                    variants =
                        mutableMapOf(
                            originalVariantAttributeKey to it.variant,
                        ),
                ),
            )
            idReference.put(it.asset.id, it)
            currentEntryId++
        }
    }

    override suspend fun fetchOriginalVariant(id: UUID): AssetAndVariant? {
        return idReference[id]
    }

    override suspend fun fetchByPath(
        treePath: String,
        entryId: Long?,
        imageAttributes: ImageAttributes?,
    ): AssetAndVariant? {
        return store[treePath]?.let { assets ->
            val resolvedEntryId = entryId ?: assets.maxByOrNull { it.asset.createdAt }?.asset?.entryId
            val asset = assets.firstOrNull { it.asset.entryId == resolvedEntryId }
            asset?.let {
                AssetAndVariant(
                    asset = it.asset,
                    variant = it.variants[it.originalVariantAttributeKey]!!,
                )
            }
        }
    }

    override suspend fun fetchAllByPath(treePath: String): List<AssetAndVariant> {
        return store[treePath]?.toList()?.sortedBy { it.asset.entryId }?.reversed()?.map {
            AssetAndVariant(
                asset = it.asset,
                variant = it.variants[it.originalVariantAttributeKey]!!,
            )
        } ?: emptyList()
    }

    override suspend fun deleteAssetByPath(
        treePath: String,
        entryId: Long?,
    ) {
        logger.info("Deleting asset at path: $treePath and entryId: ${entryId ?: "not specified"}")
        val asset = fetchByPath(treePath, entryId, null)
        asset?.let {
            idReference.remove(it.asset.id)
        }
        store[treePath]?.let { assets ->
            val resolvedEntryId = entryId ?: assets.maxByOrNull { it.asset.createdAt }?.asset?.entryId
            resolvedEntryId?.let {
                assets.removeIf { it.asset.entryId == resolvedEntryId }
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
                store[path]?.map { it.asset.id }?.forEach {
                    idReference.remove(it)
                }
                store.remove(path)
            }
        } else {
            logger.info("Deleting assets at path: $treePath")
            store[treePath]?.map { it.asset.id }?.forEach {
                idReference.remove(it)
            }
            store.remove(treePath)
        }
    }
}

private data class AssetAndVariants(
    val asset: Asset,
    val originalVariantAttributeKey: String,
    val variants: MutableMap<String, AssetVariant> = mutableMapOf(),
)

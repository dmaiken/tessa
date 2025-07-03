package io.asset.repository

import asset.Asset
import io.asset.AssetAndVariants
import io.asset.VariantBucketAndKey
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
    private val store = mutableMapOf<String, MutableList<InMemoryAssetAndVariants>>()
    private val idReference = mutableMapOf<UUID, Asset>()

    override suspend fun store(asset: StoreAssetDto): AssetAndVariants {
        val entryId = getNextEntryId(asset.treePath)
        logger.info("Persisting asset at path: ${asset.treePath} and entryId: $entryId")
        val assetAndVariants =
            AssetAndVariants(
                asset =
                    Asset(
                        id = UUID.randomUUID(),
                        alt = asset.request.alt,
                        entryId = entryId,
                        path = asset.treePath,
                        createdAt = LocalDateTime.now(),
                    ),
                variants =
                    listOf(
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
                    ),
            )
        return assetAndVariants.also {
            val originalVariantAttributeKey =
                variantParameterGenerator.generateImageVariantAttributes(asset.imageAttributes)
            store.computeIfAbsent(asset.treePath) { mutableListOf() }.add(
                InMemoryAssetAndVariants(
                    asset = it.asset,
                    originalVariantAttributeKey = originalVariantAttributeKey,
                    variants =
                        mutableMapOf(
                            originalVariantAttributeKey to it.variants.first(),
                        ),
                ),
            )
            idReference.put(it.asset.id, it.asset)
        }
    }

    override suspend fun fetchByPath(
        treePath: String,
        entryId: Long?,
        imageAttributes: ImageAttributes?,
    ): AssetAndVariants? {
        return store[treePath]?.let { assets ->
            val resolvedEntryId = entryId ?: assets.maxByOrNull { it.asset.createdAt }?.asset?.entryId
            val asset = assets.firstOrNull { it.asset.entryId == resolvedEntryId }
            asset?.let {
                AssetAndVariants(
                    asset = it.asset,
                    variants = it.variants.values.toList(),
                )
            }
        }
    }

    override suspend fun fetchAllByPath(treePath: String): List<AssetAndVariants> {
        return store[treePath]?.toList()?.sortedBy { it.asset.entryId }?.reversed()?.map {
            AssetAndVariants(
                asset = it.asset,
                variants = it.variants.values.toList(),
            )
        } ?: emptyList()
    }

    override suspend fun deleteAssetByPath(
        treePath: String,
        entryId: Long?,
    ): List<VariantBucketAndKey> {
        logger.info("Deleting asset at path: $treePath and entryId: ${entryId ?: "not specified"}")

        val asset =
            store[treePath]?.let { assets ->
                val resolvedEntryId = entryId ?: assets.maxByOrNull { it.asset.createdAt }?.asset?.entryId
                assets.firstOrNull { it.asset.entryId == resolvedEntryId }
            }

        asset?.let {
            idReference.remove(it.asset.id)
        }
        store[treePath]?.let { assets ->
            val resolvedEntryId = entryId ?: assets.maxByOrNull { it.asset.createdAt }?.asset?.entryId
            resolvedEntryId?.let {
                assets.removeIf { it.asset.entryId == resolvedEntryId }
            }
        }
        return asset?.variants?.map {
            VariantBucketAndKey(
                bucket = it.value.objectStoreBucket,
                key = it.value.objectStoreKey,
            )
        } ?: emptyList()
    }

    override suspend fun deleteAssetsByPath(
        treePath: String,
        recursive: Boolean,
    ): List<VariantBucketAndKey> {
        val objectStoreInformation = mutableListOf<VariantBucketAndKey>()
        if (recursive) {
            logger.info("Deleting assets (recursively) at path: $treePath")
            store.keys.filter { it.startsWith(treePath) }.forEach { path ->
                val assetAndVariants = store[path]
                assetAndVariants?.forEach {
                    objectStoreInformation.addAll(mapToBucketAndKey(it))
                }
                assetAndVariants?.map { it.asset.id }?.forEach {
                    idReference.remove(it)
                }
                store.remove(path)
            }
        } else {
            logger.info("Deleting assets at path: $treePath")
            val assetAndVariants = store[treePath]
            assetAndVariants?.forEach {
                objectStoreInformation.addAll(mapToBucketAndKey(it))
            }
            assetAndVariants?.map { it.asset.id }?.forEach {
                idReference.remove(it)
            }
            store.remove(treePath)
        }

        return objectStoreInformation
    }

    private fun mapToBucketAndKey(assetAndVariants: InMemoryAssetAndVariants): List<VariantBucketAndKey> {
        return assetAndVariants.variants.values.map { variant ->
            VariantBucketAndKey(
                bucket = variant.objectStoreBucket,
                key = variant.objectStoreKey,
            )
        }
    }

    private fun getNextEntryId(treePath: String): Long {
        return store[treePath]?.maxByOrNull { it.asset.entryId }?.asset?.entryId?.inc() ?: 0
    }

    private data class InMemoryAssetAndVariants(
        val asset: Asset,
        val originalVariantAttributeKey: String,
        val variants: MutableMap<String, AssetVariant> = mutableMapOf(),
    )
}

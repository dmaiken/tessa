package asset.repository

import asset.variant.VariantParameterGenerator

class InMemoryAssetRepositoryTest : AssetRepositoryTest() {
    override fun createRepository(): AssetRepository {
        return InMemoryAssetRepository(VariantParameterGenerator())
    }
}

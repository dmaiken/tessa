package io.asset.repository

import asset.StoreAssetRequest
import asset.store.PersistResult
import io.asset.handler.StoreAssetDto
import io.image.ImageAttributes
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.util.UUID

class InMemoryAssetRepositoryTest {
    private val repository: AssetRepository = InMemoryAssetRepository()

    @Test
    fun `can store and fetch an asset`() =
        runTest {
            val dto = createAssetDto("root.users.123")
            val asset = repository.store(dto)
            val fetched = repository.fetch(asset.id)

            fetched shouldBe asset
        }

    @Test
    fun `fetching asset that does not exist returns null`() =
        runTest {
            repository.fetch(UUID.randomUUID()) shouldBe null
        }

    @Test
    fun `storing an asset on an existent tree path appends the asset and increments entryId`() =
        runTest {
            val dto1 = createAssetDto("root.users.123")
            val dto2 = createAssetDto("root.users.123")
            val asset1 = repository.store(dto1)
            val asset2 = repository.store(dto2)

            asset1.entryId shouldBe 0
            asset2.entryId shouldBe 1
        }

    @Test
    fun `fetchByPath returns an existing asset`() =
        runTest {
            val dto = createAssetDto("root.users.123")
            val asset = repository.store(dto)
            val fetched = repository.fetch(asset.id)

            fetched shouldBe asset
        }

    @Test
    fun `fetchByPath returns last created asset if multiple exist`() =
        runTest {
            val dto1 = createAssetDto("root.users.123")
            val dto2 = createAssetDto("root.users.123")
            repository.store(dto1)
            val asset2 = repository.store(dto2)

            repository.fetchByPath("root.users.123", entryId = null) shouldBe asset2
        }

    @Test
    fun `fetchByPath returns an existing asset by entryId`() =
        runTest {
            val dto1 = createAssetDto("root.users.123")
            val dto2 = createAssetDto("root.users.123")
            val asset1 = repository.store(dto1)
            val asset2 = repository.store(dto2)

            repository.fetchByPath("root.users.123", entryId = 0) shouldBe asset1
            repository.fetchByPath("root.users.123", entryId = 1) shouldBe asset2
        }

    @Test
    fun `fetchByPath returns null if there is no asset in path`() =
        runTest {
            repository.fetchByPath("root.users.123", entryId = null) shouldBe null
        }

    @Test
    fun `fetchByPath returns null if there is no asset in path at specific entryId`() =
        runTest {
            val dto = createAssetDto("root.users.123")
            repository.store(dto)
            repository.fetchByPath("root.users.123", entryId = 1) shouldBe null
        }

    @Test
    fun `fetchAllByPath returns asset at path`() =
        runTest {
            val dto1 = createAssetDto("root.users.123")
            val asset1 = repository.store(dto1)

            repository.fetchAllByPath("root.users.123") shouldBe listOf(asset1)
        }

    @Test
    fun `fetchAllByPath returns all assets at path`() =
        runTest {
            val dto1 = createAssetDto("root.users.123")
            val dto2 = createAssetDto("root.users.123")
            val asset1 = repository.store(dto1)
            val asset2 = repository.store(dto2)

            repository.fetchAllByPath("root.users.123") shouldBe listOf(asset2, asset1)
        }

    @Test
    fun `fetchAllByPath returns empty list if no assets in path`() =
        runTest {
            repository.fetchAllByPath("root.users.123") shouldBe emptyList()
        }

    @Test
    fun `deleteAssetByPath deletes the asset`() =
        runTest {
            val dto = createAssetDto("root.users.123")
            val asset = repository.store(dto)
            repository.deleteAssetByPath("root.users.123")

            repository.fetch(asset.id) shouldBe null
            repository.fetchByPath("root.users.123", entryId = null) shouldBe null
        }

    @Test
    fun `deleteAssetByPath returns does nothing if asset does not exist`() =
        runTest {
            shouldNotThrowAny {
                repository.deleteAssetByPath("root.users.123")
            }
        }

    @Test
    fun `deleteAssetByPath returns does nothing if asset does not exist at specific entryId`() =
        runTest {
            val dto = createAssetDto("root.users.123")
            val asset = repository.store(dto)
            shouldNotThrowAny {
                repository.deleteAssetByPath("root.users.123", entryId = 1)
            }

            repository.fetch(asset.id) shouldBe asset
            repository.fetchAllByPath("root.users.123") shouldBe listOf(asset)
        }

    @Test
    fun `deleteAssetsByPath deletes all assets at path`() =
        runTest {
            val dto1 = createAssetDto("root.users.123")
            val dto2 = createAssetDto("root.users.123")
            val asset1 = repository.store(dto1)
            val asset2 = repository.store(dto2)

            repository.deleteAssetsByPath("root.users.123", recursive = false)

            repository.fetch(asset1.id) shouldBe null
            repository.fetch(asset2.id) shouldBe null
            repository.fetchAllByPath("root.users.123") shouldBe emptyList()
        }

    @Test
    fun `deleteAssetsByPath deletes all assets at path and under if recursive delete`() =
        runTest {
            val dto1 = createAssetDto("root.users.123")
            val dto2 = createAssetDto("root.users.123")
            val dto3 = createAssetDto("root.users.123.profile")
            val asset1 = repository.store(dto1)
            val asset2 = repository.store(dto2)
            val asset3 = repository.store(dto3)

            repository.deleteAssetsByPath("root.users.123", recursive = true)
            repository.fetch(asset1.id) shouldBe null
            repository.fetch(asset2.id) shouldBe null
            repository.fetch(asset3.id) shouldBe null
            repository.fetchAllByPath("root.users.123") shouldBe emptyList()
            repository.fetchAllByPath("root.users.123.profile") shouldBe emptyList()
        }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `deleteAssetsByPath does nothing if nothing exists at path`(recursive: Boolean) =
        runTest {
            shouldNotThrowAny {
                repository.deleteAssetsByPath("root.users.123", recursive)
            }
        }

    @Test
    fun `entryId always increases`() =
        runTest {
            val dto1 = createAssetDto("root.users.123")
            val dto2 = createAssetDto("root.users.123")
            val asset1 = repository.store(dto1)
            val asset2 = repository.store(dto2)
            asset1.entryId shouldBe 0
            asset2.entryId shouldBe 1
            repository.deleteAssetByPath("root.users.123")

            val dto3 = createAssetDto("root.users.123")
            val asset3 = repository.store(dto3)
            asset3.entryId shouldBe 2
        }

    private fun createAssetDto(treePath: String): StoreAssetDto {
        return StoreAssetDto(
            mimeType = "image/png",
            treePath = treePath,
            request =
                StoreAssetRequest(
                    fileName = "hello.png",
                    type = "image.png",
                    alt = "an image",
                ),
            imageAttributes =
                ImageAttributes(
                    mimeType = "image/png",
                    width = 100,
                    height = 100,
                ),
            persistResult =
                PersistResult(
                    key = UUID.randomUUID().toString(),
                    bucket = "bucket",
                    url = "https://example.com",
                ),
        )
    }
}

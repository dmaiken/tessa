package io.inmemory

import asset.StoreAssetRequest
import asset.store.ObjectStore
import io.asset.store.InMemoryObjectStore
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream
import java.util.UUID

class InMemoryObjectStoreTest {
    private val store: ObjectStore = InMemoryObjectStore()

    @Test
    fun `can persist and fetch an object`() =
        runTest {
            val request = createStoreAssetRequest()
            val bytes = UUID.randomUUID().toString().toByteArray()

            val result = store.persist(request, bytes)
            result.bucket shouldBe InMemoryObjectStore.BUCKET

            val stream = ByteArrayOutputStream()
            val fetchResult = store.fetch(result.bucket, result.key, stream)
            fetchResult.found shouldBe true
            fetchResult.contentLength shouldBe bytes.size.toLong()
            stream.use {
                stream.toByteArray() shouldBe bytes
            }
        }

    @Test
    fun `can fetch if the object does not exist`() =
        runTest {
            val stream = ByteArrayOutputStream()
            val fetchResult = store.fetch("something", UUID.randomUUID().toString(), stream)
            fetchResult.found shouldBe false
            fetchResult.contentLength shouldBe 0
            stream.use {
                stream.toByteArray() shouldHaveSize 0
            }
        }

    @Test
    fun `can delete an object`() =
        runTest {
            val request = createStoreAssetRequest()
            val bytes = UUID.randomUUID().toString().toByteArray()
            val result = store.persist(request, bytes)

            store.delete(result.bucket, result.key)

            val stream = ByteArrayOutputStream()
            val fetchResult = store.fetch(result.bucket, result.key, stream)
            fetchResult.found shouldBe false
            fetchResult.contentLength shouldBe 0
            stream.use {
                stream.toByteArray() shouldHaveSize 0
            }
        }

    @Test
    fun `can delete if object does not exist`() =
        runTest {
            shouldNotThrowAny {
                store.delete("something", UUID.randomUUID().toString())
            }
        }

    @Test
    fun `deleteAll deletes supplied objects in bucket`() =
        runTest {
            val request1 = createStoreAssetRequest()
            val bytes1 = UUID.randomUUID().toString().toByteArray()
            val result1 = store.persist(request1, bytes1)

            val request2 = createStoreAssetRequest()
            val bytes2 = UUID.randomUUID().toString().toByteArray()
            val result2 = store.persist(request2, bytes2)

            val request3 = createStoreAssetRequest()
            val bytes3 = UUID.randomUUID().toString().toByteArray()
            val result3 = store.persist(request3, bytes3)

            result1.bucket shouldBe result2.bucket shouldBe result3.bucket
            store.deleteAll(result1.bucket, listOf(result1.key, result2.key, result3.key))

            val stream = ByteArrayOutputStream()
            store.fetch(result1.bucket, result1.key, stream).apply {
                found shouldBe false
                contentLength shouldBe 0
                stream.use {
                    stream.toByteArray() shouldHaveSize 0
                }
            }
            stream.reset()
            store.fetch(result2.bucket, result2.key, stream).apply {
                found shouldBe false
                contentLength shouldBe 0
                stream.use {
                    stream.toByteArray() shouldHaveSize 0
                }
            }
            stream.reset()
            store.fetch(result3.bucket, result3.key, stream).apply {
                found shouldBe false
                contentLength shouldBe 0
                stream.use {
                    stream.toByteArray() shouldHaveSize 0
                }
            }
        }

    @Test
    fun `deleteAll does nothing if wrong bucket is supplied`() =
        runTest {
            val request = createStoreAssetRequest()
            val bytes = UUID.randomUUID().toString().toByteArray()
            val result = store.persist(request, bytes)

            store.deleteAll(result.bucket + "1", listOf(result.key))

            val stream = ByteArrayOutputStream()
            store.fetch(result.bucket, result.key, stream).apply {
                found shouldBe true
                contentLength shouldBe bytes.size
                stream.use {
                    stream.toByteArray() shouldHaveSize bytes.size
                }
            }
        }

    @Test
    fun `can deleteAll if keys do not exist in bucket`() =
        runTest {
            val request = createStoreAssetRequest()
            val bytes = UUID.randomUUID().toString().toByteArray()
            val result = store.persist(request, bytes)

            store.deleteAll(result.bucket, listOf(UUID.randomUUID().toString()))

            val stream = ByteArrayOutputStream()
            store.fetch(result.bucket, result.key, stream).apply {
                found shouldBe true
                contentLength shouldBe bytes.size
                stream.use {
                    stream.toByteArray() shouldHaveSize bytes.size
                }
            }
        }

    private fun createStoreAssetRequest(): StoreAssetRequest =
        StoreAssetRequest(
            fileName = "${UUID.randomUUID()}.jpeg",
            type = "image/jpeg",
            alt = "an image",
        )
}

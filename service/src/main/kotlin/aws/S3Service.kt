package io.aws

import asset.store.FetchResult
import asset.store.ObjectStore
import asset.store.PersistResult
import asset.variant.AssetVariant
import aws.sdk.kotlin.services.s3.S3Client
import aws.sdk.kotlin.services.s3.model.Delete
import aws.sdk.kotlin.services.s3.model.DeleteObjectRequest
import aws.sdk.kotlin.services.s3.model.DeleteObjectsRequest
import aws.sdk.kotlin.services.s3.model.GetObjectRequest
import aws.sdk.kotlin.services.s3.model.NoSuchKey
import aws.sdk.kotlin.services.s3.model.ObjectIdentifier
import aws.sdk.kotlin.services.s3.model.PutObjectRequest
import aws.smithy.kotlin.runtime.content.ByteStream
import aws.smithy.kotlin.runtime.content.fromInputStream
import aws.smithy.kotlin.runtime.content.writeToOutputStream
import io.ktor.util.logging.KtorSimpleLogger
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

class S3Service(
    private val s3Client: S3Client,
    private val awsProperties: AWSProperties,
) : ObjectStore {
    companion object {
        const val BUCKET = "assets"
    }

    private val logger = KtorSimpleLogger(this::class.qualifiedName!!)

    override suspend fun persist(asset: InputStream): PersistResult {
        val key = UUID.randomUUID().toString()
        s3Client.putObject(
            input =
                PutObjectRequest {
                    bucket = BUCKET
                    this.key = key
                    body = ByteStream.fromInputStream(asset)
                },
        )

        return PersistResult(
            key = key,
            bucket = BUCKET,
        )
    }

    override suspend fun fetch(
        bucket: String,
        key: String,
        stream: OutputStream,
    ): FetchResult {
        return try {
            s3Client.getObject(
                input =
                    GetObjectRequest {
                        this.bucket = bucket
                        this.key = key
                    },
            ) {
                it.body?.let { body ->
                    body.writeToOutputStream(stream)
                    FetchResult.found(requireNotNull(it.contentLength))
                } ?: FetchResult.notFound()
            }
        } catch (e: NoSuchKey) {
            logger.info("Object with key $key in bucket $bucket does not exist", e)
            FetchResult.notFound()
        }
    }

    override suspend fun delete(
        bucket: String,
        key: String,
    ) {
        try {
            s3Client.deleteObject(
                input =
                    DeleteObjectRequest {
                        this.bucket = bucket
                        this.key = key
                    },
            )
        } catch (e: Exception) {
            logger.error("Unable to delete asset with key: $key from bucket: $bucket", e)
            throw e
        }
    }

    override suspend fun deleteAll(
        bucket: String,
        keys: List<String>,
    ) {
        // max 1000 keys allowed per S3 request
        keys.chunked(1000).forEach { chunk ->
            s3Client.deleteObjects(
                input =
                    DeleteObjectsRequest {
                        this.bucket = bucket
                        delete =
                            Delete {
                                objects =
                                    chunk.map {
                                        ObjectIdentifier { key = it }
                                    }
                            }
                    },
            )
        }
    }

    override fun generateObjectUrl(variant: AssetVariant): String {
        return "https://${awsProperties.host}/${variant.objectStoreBucket}" +
            "/${variant.objectStoreKey}"
    }
}

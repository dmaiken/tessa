package io.asset.store

import asset.StoreAssetRequest
import aws.sdk.kotlin.services.s3.S3Client
import aws.sdk.kotlin.services.s3.model.DeleteObjectRequest
import aws.sdk.kotlin.services.s3.model.PutObjectRequest
import aws.smithy.kotlin.runtime.content.ByteStream
import io.image.store.ObjectStore
import io.image.store.PersistResult
import io.ktor.util.logging.KtorSimpleLogger
import java.util.*

class S3Service(
    private val s3Client: S3Client,
    private val awsProperties: AWSProperties
) : ObjectStore {

    companion object {
        const val BUCKET = "assets"
    }

    val logger = KtorSimpleLogger(this::class.qualifiedName!!)

    override suspend fun persist(data: StoreAssetRequest, image: ByteArray): PersistResult {
        val key = UUID.randomUUID().toString()
        s3Client.putObject(
            input = PutObjectRequest {
                bucket = BUCKET
                this.key = key
                body = ByteStream.fromBytes(image)
            }
        )

        return PersistResult(
            key = key,
            bucket = BUCKET,
            url = createS3Url(key)
        )
    }

    override suspend fun delete(bucket: String, key: String) {
        try {
            s3Client.deleteObject(
                input = DeleteObjectRequest {
                    this.bucket = bucket
                    this.key = key
                }
            )
        } catch (e: Exception) {
            logger.error("Unable to delete asset with key: $key from bucket: $bucket", e)
            throw e
        }
    }

    private fun createS3Url(key: String) = "https://${awsProperties.host}/$BUCKET/$key"
}
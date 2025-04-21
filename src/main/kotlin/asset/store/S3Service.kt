package io.asset.store

import aws.sdk.kotlin.services.s3.S3Client
import aws.sdk.kotlin.services.s3.model.PutObjectRequest
import aws.smithy.kotlin.runtime.content.ByteStream
import io.image.StoreAssetRequest
import io.image.store.ObjectStore
import io.image.store.PersistResult
import java.util.*

class S3Service(
    private val s3Client: S3Client,
    private val awsProperties: AWSProperties
) : ObjectStore {

    companion object {
        const val BUCKET = "assets"
    }

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

    private fun createS3Url(key: String) = "https://${awsProperties.host}/$BUCKET/$key"
}
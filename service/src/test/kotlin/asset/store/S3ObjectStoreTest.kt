package asset.store

import aws.sdk.kotlin.runtime.auth.credentials.StaticCredentialsProvider
import aws.sdk.kotlin.services.s3.S3Client
import aws.sdk.kotlin.services.s3.model.CreateBucketRequest
import aws.smithy.kotlin.runtime.auth.awscredentials.Credentials
import aws.smithy.kotlin.runtime.net.url.Url
import io.aws.AWSProperties
import io.aws.S3Service
import io.createImageBucket
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.testcontainers.containers.localstack.LocalStackContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName

@Testcontainers
class S3ObjectStoreTest : ObjectStoreTest() {
    companion object {
        @JvmStatic
        @Container
        private val localstack =
            LocalStackContainer(DockerImageName.parse("localstack/localstack:3.5.0"))
                .withServices(LocalStackContainer.Service.S3)

        @JvmStatic
        @BeforeAll
        fun startContainer() {
            localstack.start()
        }

        @JvmStatic
        @AfterAll
        fun stopContainer() {
            localstack.stop()
        }
    }

    override fun createObjectStore(): ObjectStore {
        val s3Client =
            S3Client {
                credentialsProvider =
                    StaticCredentialsProvider(
                        Credentials(
                            localstack.accessKey, localstack.secretKey,
                        ),
                    )
                endpointUrl = Url.parse(localstack.endpoint.toString())
                region = localstack.region
            }
        createImageBucket(s3Client)
        // Create bucket for test
        runBlocking {
            s3Client.createBucket(
                CreateBucketRequest {
                    bucket = "something"
                },
            )
            s3Client.createBucket(
                CreateBucketRequest {
                    bucket = "somethingelse"
                },
            )
        }
        return S3Service(
            s3Client = s3Client,
            awsProperties =
                AWSProperties(
                    host = "localhost.localstack.cloud:${localstack.firstMappedPort}",
                    region = localstack.region,
                ),
        )
    }
}

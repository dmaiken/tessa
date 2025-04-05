package io

import com.typesafe.config.ConfigFactory
import io.image.ImageResponse
import io.image.StoreImageRequest
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.config.*
import io.ktor.server.testing.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.*

class ApplicationTest {

    companion object {

        lateinit var postgres: PostgresTestContainerManager

        @BeforeAll
        @JvmStatic
        fun setup() {
            postgres = PostgresTestContainerManager()
        }
    }

    @Test
    fun `can create and get image`() = testApplication {
        environment {
            config = HoconApplicationConfig(ConfigFactory.load()).mergeWith(
                MapApplicationConfig(
                    "postgres.port" to postgres.getPort().toString()
                )
            )
        }
        val client = createClient {
            install(ContentNegotiation) {
                json()
            }
        }
        val id = UUID.randomUUID()
        client.post("/images") {
            contentType(ContentType.Application.Json)
            setBody(
                StoreImageRequest(
                    id = id,
                    fileName = "filename.jpeg",
                    type = "image/jpeg",
                    alt = "an image",
                    createdAt = LocalDateTime.now(),
                )
            )
        }.apply {
            status shouldBe HttpStatusCode.Created
            body<ImageResponse>().apply {
                this.id shouldBe id
                createdAt shouldNotBe null
                fileName shouldBe "filename.jpeg"
                type shouldBe "image/jpeg"
                alt shouldBe "an image"
            }
        }
        client.get("/images/$id").apply {
            status shouldBe HttpStatusCode.OK
            body<ImageResponse>().apply {
                this.id shouldBe id
                createdAt shouldNotBe null
                fileName shouldBe "filename.jpeg"
                type shouldBe "image/jpeg"
                alt shouldBe "an image"
            }
        }
    }

}

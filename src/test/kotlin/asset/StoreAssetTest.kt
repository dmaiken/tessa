package io.asset

import asset.StoreAssetRequest
import io.BaseTest
import io.config.testWithTestcontainers
import io.kotest.matchers.shouldBe
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.util.createJsonClient
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test

class StoreAssetTest : BaseTest() {

    @Test
    fun `uploading something not an image will return bad request`() = testWithTestcontainers(postgres, localstack) {
        val client = createJsonClient()
        val image = "I am not an image".toByteArray()
        val request = StoreAssetRequest(
            fileName = "filename.png",
            type = "image/png",
            alt = "an image",
        )
        client.post("/assets") {
            contentType(ContentType.MultiPart.FormData)
            setBody(
                MultiPartFormDataContent(
                    formData {
                        append("metadata", Json.encodeToString<StoreAssetRequest>(request), Headers.build {
                            append(HttpHeaders.ContentType, "application/json")
                        })
                        append("file", image, Headers.build {
                            append(HttpHeaders.ContentType, "image/png")
                            append(HttpHeaders.ContentDisposition, "filename=\"ktor_logo.png\"")
                        })
                    },
                    BOUNDARY,
                    ContentType.MultiPart.FormData.withParameter("boundary", BOUNDARY)
                )
            )
        }.apply {
            status shouldBe HttpStatusCode.BadRequest
        }
    }
}
package io.util

import asset.StoreAssetRequest
import io.BaseTest.Companion.BOUNDARY
import io.image.AssetResponse
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import kotlinx.serialization.json.Json

suspend fun storeAsset(
    client: HttpClient,
    asset: ByteArray,
    request: StoreAssetRequest,
    path: String = "profile"
): AssetResponse {
    val body: AssetResponse
    client.post("/assets/$path") {
        contentType(ContentType.MultiPart.FormData)
        setBody(
            MultiPartFormDataContent(
                formData {
                    append("metadata", Json.encodeToString<StoreAssetRequest>(request), Headers.build {
                        append(HttpHeaders.ContentType, "application/json")
                    })
                    append("file", asset, Headers.build {
                        append(HttpHeaders.ContentType, "image/png")
                        append(HttpHeaders.ContentDisposition, "filename=\"ktor_logo.png\"")
                    })
                },
                BOUNDARY,
                ContentType.MultiPart.FormData.withParameter("boundary", BOUNDARY)
            )
        )
    }.apply {
        status shouldBe HttpStatusCode.Created
        body = body<AssetResponse>().apply {
            id shouldNotBe null
            createdAt shouldNotBe null
        }
    }
    return body
}
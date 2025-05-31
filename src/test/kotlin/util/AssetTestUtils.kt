package io.util

import asset.AssetResponse
import asset.StoreAssetRequest
import io.BaseTest.Companion.BOUNDARY
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsBytes
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.serialization.json.Json

suspend fun storeAsset(
    client: HttpClient,
    asset: ByteArray,
    request: StoreAssetRequest,
    path: String = "profile",
): AssetResponse {
    val body: AssetResponse
    client.post("/assets/$path") {
        contentType(ContentType.MultiPart.FormData)
        setBody(
            MultiPartFormDataContent(
                formData {
                    append(
                        "metadata",
                        Json.encodeToString<StoreAssetRequest>(request),
                        Headers.build {
                            append(HttpHeaders.ContentType, "application/json")
                        },
                    )
                    append(
                        "file",
                        asset,
                        Headers.build {
                            append(HttpHeaders.ContentType, "image/png")
                            append(HttpHeaders.ContentDisposition, "filename=\"ktor_logo.png\"")
                        },
                    )
                },
                BOUNDARY,
                ContentType.MultiPart.FormData.withParameter("boundary", BOUNDARY),
            ),
        )
    }.apply {
        status shouldBe HttpStatusCode.Created
        body =
            body<AssetResponse>().apply {
                id shouldNotBe null
                createdAt shouldNotBe null
            }
    }
    return body
}

suspend fun fetchAsset(
    client: HttpClient,
    path: String = "profile",
    entryId: Long? = null,
): ByteArray {
    val fetchResponse =
        if (entryId != null) {
            "/assets/$path?entryId=$entryId"
        } else {
            "/assets/$path"
        }.let {
            client.get("/assets/$path/").apply {
                status shouldBe HttpStatusCode.TemporaryRedirect
                headers["Location"] shouldContain "https://"
            }
        }
    val generalClient = createGeneralClient()
    val storeResponse = generalClient.get(fetchResponse.headers["Location"]!!)
    storeResponse.status shouldBe HttpStatusCode.OK

    return storeResponse.bodyAsBytes()
}

suspend fun fetchAssetInfo(
    client: HttpClient,
    path: String,
    entryId: Long? = null,
    expectedResponse: HttpStatusCode = HttpStatusCode.OK,
): AssetResponse? {
    return if (entryId != null) {
        "/assets/$path?format=metadata&entryId=$entryId"
    } else {
        "/assets/$path?format=metadata"
    }.let {
        val response = client.get("/assets/$path?format=metadata")
        response.status shouldBe expectedResponse

        if (response.status == HttpStatusCode.NotFound) {
            null
        } else {
            response.body<AssetResponse>().apply {
                entryId shouldBe entryId
            }
        }
    }
}

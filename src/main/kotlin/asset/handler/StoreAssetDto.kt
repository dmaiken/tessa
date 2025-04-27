package io.asset.handler

import asset.StoreAssetRequest

data class StoreAssetDto(
    val content: ByteArray,
    val mimeType: String,
    val treePath: String,
    val request: StoreAssetRequest,
)

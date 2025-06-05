package io.asset.handler

import asset.StoreAssetRequest

class StoreAssetDto(
    val content: ByteArray,
    val mimeType: String,
    val treePath: String,
    val request: StoreAssetRequest,
)

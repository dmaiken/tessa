package io.asset.handler

import asset.StoreAssetRequest
import asset.store.PersistResult
import io.image.ImageAttributes

class StoreAssetDto(
    val mimeType: String,
    val treePath: String,
    val request: StoreAssetRequest,
    val imageAttributes: ImageAttributes,
    val persistResult: PersistResult,
)

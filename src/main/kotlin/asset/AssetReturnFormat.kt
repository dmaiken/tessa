package io.asset

enum class AssetReturnFormat {

    CONTENT,
    METADATA,
    REDIRECT;

    companion object {
        fun fromQueryParam(value: String?) = value?.let {
            valueOf(value.uppercase())
        } ?: REDIRECT // Default
    }
}
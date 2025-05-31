package io.asset

import java.util.Locale.getDefault

enum class PathModifierOption {
    CHILDREN,
    RECURSIVE,
    ;

    companion object {
        fun fromString(option: String): PathModifierOption {
            return try {
                valueOf(option.uppercase(getDefault()))
            } catch (_: IllegalArgumentException) {
                CHILDREN
            }
        }
    }
}

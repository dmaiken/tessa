package io

import io.asset.PathModifierOption
import io.ktor.server.routing.RoutingRequest

/**
 * Gets the entryId of the request.
 *
 * @throws IllegalArgumentException if the entryId supplied is not a valid, non-negative [Long]
 */
fun getEntryId(request: RoutingRequest): Long? {
    val suppliedEntryId = request.queryParameters["entryId"]
    return if (suppliedEntryId != null) {
        return try {
            val entryId = suppliedEntryId.toLong()
            if (entryId < 0) {
                throw IllegalArgumentException("entryId must be greater than 0")
            }
            entryId
        } catch (e: NumberFormatException) {
            throw IllegalArgumentException("entryId must be a valid number", e)
        }
    } else {
        null
    }
}

fun getPathModifierOption(request: RoutingRequest): PathModifierOption? {
    return request.queryParameters["mode"]?.let { option ->
        PathModifierOption.fromString(option)
    }
}

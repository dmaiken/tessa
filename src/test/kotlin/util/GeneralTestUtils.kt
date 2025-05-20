package io.util

import io.ktor.client.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.testing.*

fun ApplicationTestBuilder.createJsonClient(): HttpClient = createClient {
    install(ContentNegotiation) { json() }
}
package io.util

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.testing.ApplicationTestBuilder
import java.security.cert.X509Certificate
import javax.net.ssl.X509TrustManager

fun ApplicationTestBuilder.createJsonClient(followRedirects: Boolean = true): HttpClient =
    createClient {
        this.followRedirects = followRedirects
        install(ContentNegotiation) {
            json()
        }
    }

fun createGeneralClient(): HttpClient {
    return HttpClient(CIO) {
        engine {
            https {
                trustManager = AllCertsTrustManager()
            }
        }
    }
}

private class AllCertsTrustManager : X509TrustManager {
    @Suppress("TrustAllX509TrustManager")
    override fun checkServerTrusted(
        chain: Array<X509Certificate>,
        authType: String,
    ) {
        // no-op
    }

    @Suppress("TrustAllX509TrustManager")
    override fun checkClientTrusted(
        chain: Array<X509Certificate>,
        authType: String,
    ) {
        // no-op
    }

    override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
}

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.ktor)
    alias(libs.plugins.kotlin.plugin.serialization)
    alias(libs.plugins.ktlint)
}

group = "io"
version = "0.0.1"

application {
    mainClass = "io.ktor.server.netty.EngineMain"

    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.jooq)
    implementation(libs.jooq.kotlin)
    implementation(libs.jooq.kotlin.coroutines)
    implementation(libs.jooq.postgres.extensions)
    implementation(libs.koin.ktor)
    implementation(libs.koin.logger.slf4j)
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.server.netty)
    implementation(libs.logback.classic)
    implementation(libs.ktor.server.status.pages)
    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.kotlin.test.junit)
    testImplementation(libs.mockk)

    implementation(libs.r2dbc.migrate)
    implementation(libs.r2dbc.migrate.resource.reader)
    implementation(libs.r2dbc.postgresql)
    implementation(libs.r2dbc.pool)
    implementation(libs.kotlinx.coroutines.reactive)
    testImplementation(libs.ktor.client.content.negotiation)

    testImplementation(libs.testcontainers)
    testImplementation(libs.testcontainers.postgresql)
    testImplementation(libs.testcontainers.localstack)

    testImplementation(libs.kotest.runner)
    testImplementation(libs.kotest.assertions)

    implementation(awssdk.services.s3)

    implementation(libs.libvips.ffm)
    implementation("org.apache.tika:tika-core:3.1.0")
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    systemProperty("kotest.extensions.autoscan.disable", "true")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(23)
    }
}

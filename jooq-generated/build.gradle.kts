plugins {
    alias(libs.plugins.kotlin.jvm)
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.jooq)
    implementation(libs.jooq.kotlin)
    implementation(libs.jooq.kotlin.coroutines)
    implementation(libs.jooq.postgres.extensions)
}

// Disable linting for this module
tasks.matching { it.name.startsWith("ktlint") }.configureEach {
    enabled = false
}

kotlin {
    jvmToolchain(23)
}

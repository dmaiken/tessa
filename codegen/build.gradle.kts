plugins {
    kotlin("jvm")
    application
}

version = "0.0.1"

application {
    mainClass.set("CodeGenKt")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.r2dbc.migrate)
    implementation(libs.r2dbc.migrate.resource.reader)
    implementation(libs.r2dbc.postgresql)
    implementation(libs.logback.classic)

    // jOOQ (codegen only)
    implementation(libs.jooq.codegen)
    implementation(libs.jooq.codegen.meta.extensions)

    implementation("org.postgresql:postgresql:42.7.2")

    implementation(libs.testcontainers.postgresql)
    implementation(project(":service"))
}

tasks.register("generateJooq") {
    dependsOn("run")
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(23)
}

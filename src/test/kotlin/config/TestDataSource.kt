package io.config

import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName

class PostgresTestContainer : PostgreSQLContainer<PostgresTestContainer>(
    DockerImageName.parse("postgres:17.4")
)

class PostgresTestContainerManager {
    private var started = false
    private val postgres = PostgresTestContainer().apply {
        withDatabaseName("imagek")
        withUsername("username")
        withPassword("password")
    }

    init {
        postgres.start()
        started = true
    }

    fun getPort(): Int = postgres.getMappedPort(5432)

    fun stop() = postgres.stop()
}

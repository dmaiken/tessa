package io

import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll

open class BaseTest {

    companion object {

        lateinit var postgres: PostgresTestContainerManager
        lateinit var localstack: LocalstackContainerManager

        @BeforeAll
        @JvmStatic
        fun setup() {
            postgres = PostgresTestContainerManager()
            localstack = LocalstackContainerManager()
        }

        @AfterAll
        @JvmStatic
        fun tearDown() {
            postgres.stop()
            localstack.stop()
        }

        const val BOUNDARY = "boundary"
    }
}
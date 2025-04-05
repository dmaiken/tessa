package io

import io.ktor.server.application.*
import io.r2dbc.spi.ConnectionFactories
import io.r2dbc.spi.ConnectionFactory
import io.r2dbc.spi.ConnectionFactoryOptions.*
import name.nkonev.r2dbc.migrate.core.R2dbcMigrate
import name.nkonev.r2dbc.migrate.core.R2dbcMigrateProperties
import name.nkonev.r2dbc.migrate.reader.ReflectionsClasspathResourceReader
import org.jooq.DSLContext
import org.jooq.SQLDialect
import org.jooq.impl.DSL

fun Application.connectToPostgres(): ConnectionFactory {
    log.info("Connecting to postgres database")
    val user = environment.config.property("postgres.user").getString()
    val password = environment.config.property("postgres.password").getString()
    val host = environment.config.property("postgres.host").getString()
    val port = environment.config.property("postgres.port").getString().toInt()
    val options = builder()
        .option(DATABASE, "imagek")
        .option(DRIVER, "pool")
        .option(PROTOCOL, "postgresql")
        .option(USER, user)
        .option(PASSWORD, password)
        .option(HOST, host)
        .option(PORT, port)
        .build()


    return ConnectionFactories.get(options)
}

fun migrateSchema(connectionFactory: ConnectionFactory) {
    val migrateProperties = R2dbcMigrateProperties().apply {
        setResourcesPath("db/migration")
    }

    R2dbcMigrate.migrate(connectionFactory, migrateProperties, ReflectionsClasspathResourceReader(), null, null).block()
}

fun configureJOOQ(connectionFactory: ConnectionFactory): DSLContext = DSL.using(connectionFactory, SQLDialect.POSTGRES)

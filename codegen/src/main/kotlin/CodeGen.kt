import io.r2dbc.spi.ConnectionFactories
import io.r2dbc.spi.ConnectionFactory
import io.r2dbc.spi.ConnectionFactoryOptions.DATABASE
import io.r2dbc.spi.ConnectionFactoryOptions.DRIVER
import io.r2dbc.spi.ConnectionFactoryOptions.HOST
import io.r2dbc.spi.ConnectionFactoryOptions.PASSWORD
import io.r2dbc.spi.ConnectionFactoryOptions.PORT
import io.r2dbc.spi.ConnectionFactoryOptions.PROTOCOL
import io.r2dbc.spi.ConnectionFactoryOptions.USER
import io.r2dbc.spi.ConnectionFactoryOptions.builder
import name.nkonev.r2dbc.migrate.core.R2dbcMigrate
import name.nkonev.r2dbc.migrate.core.R2dbcMigrateProperties
import name.nkonev.r2dbc.migrate.reader.ReflectionsClasspathResourceReader
import org.testcontainers.containers.PostgreSQLContainer
import java.io.File
import org.jooq.codegen.GenerationTool
import org.jooq.meta.jaxb.Configuration
import org.jooq.meta.jaxb.Database
import org.jooq.meta.jaxb.Generate
import org.jooq.meta.jaxb.Generator
import org.jooq.meta.jaxb.Target
import org.jooq.meta.jaxb.Jdbc

fun main() {
    // 1. Start Postgres container
    val pg = PostgreSQLContainer("postgres:15").apply {
        start()
        println("PostgreSQL Container started")
    }
    val r2dbcUrl = "r2dbc:postgresql://${pg.host}:${pg.getMappedPort(5432)}/${pg.databaseName}"
    println("Running migrations on: $r2dbcUrl")

    // 2. Run migrations with r2dbc-migrate
    val options = builder()
        .option(DATABASE, pg.databaseName)
        .option(DRIVER, "postgresql")
        .option(PROTOCOL, "postgresql")
        .option(USER, pg.username)
        .option(PASSWORD, pg.password)
        .option(HOST, pg.host)
        .option(PORT, pg.getMappedPort(5432))
        .build()

    val migrateProperties =
        R2dbcMigrateProperties().apply {
            setResourcesPath("db/migration")
        }

    R2dbcMigrate.migrate(
        ConnectionFactories.get(options),
        migrateProperties,
        ReflectionsClasspathResourceReader(),
        null,
        null
    ).block()

    // 3. Dump schema using pg_dump (requires pg_dump in PATH)
    val output = File("src/main/resources/schema.sql")
    output.parentFile.mkdirs()
    val process = ProcessBuilder(
        "pg_dump",
        "--schema-only",
        "--no-owner",
        "--no-privileges",
        "-h", pg.host,
        "-p", pg.getMappedPort(5432).toString(),
        "-U", pg.username,
        "-d", pg.databaseName
    ).apply {
        environment()["PGPASSWORD"] = pg.password
        redirectOutput(output)
    }.start()
    val errorReader = process.errorStream.bufferedReader()
    val errorOutput = errorReader.readText()
    println("pg_dump stderr: $errorOutput")

    val exitCode = process.waitFor()

    if (exitCode == 0) {
        println("Schema dumped successfully to: ${output.absolutePath}")
    } else {
        println("pg_dump failed with exit code $exitCode")
    }

    val configuration = Configuration().apply {
        jdbc = Jdbc().apply {
            driver = "org.postgresql.Driver"
            url = pg.jdbcUrl
            user = pg.username
            password = pg.password
        }
        generator = Generator().apply {
            name = "org.jooq.codegen.KotlinGenerator"
            database = Database().apply {
                name = "org.jooq.meta.postgres.PostgresDatabase"
                inputSchema = "public"
            }
            target = Target().apply {
                packageName = "tessa.jooq"
                directory = "../jooq-generated/src/main/kotlin"
            }
            generate = Generate().apply {
                withKotlinNotNullInterfaceAttributes(true)
                withKotlinNotNullPojoAttributes(true)

            }
        }
    }
    GenerationTool.generate(configuration)

    pg.stop()
}
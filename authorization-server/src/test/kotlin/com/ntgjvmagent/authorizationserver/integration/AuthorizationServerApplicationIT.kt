package com.ntgjvmagent.authorizationserver.integration

import com.ntgjvmagent.authorizationserver.integration.config.PostgresTestContainer
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ContextConfiguration
import java.sql.Connection
import javax.sql.DataSource

@SpringBootTest
@ContextConfiguration(initializers = [PostgresTestContainer.Initializer::class])
class AuthorizationServerApplicationIT(
    @Autowired val dataSource: DataSource
) {

    @Test
    fun `verify Flyway migration applied`() {
        dataSource.connection.use { conn: Connection ->
            conn.createStatement().use { stmt ->
                stmt.executeQuery("SELECT * FROM users").use { rs ->
                    assert(rs.next()) { "Flyway migration not applied properly!" }
                    println("Flyway migration executed successfully, found user: ${rs.getString("username")}")
                }
            }
        }
    }

    @Test
    fun contextLoads() {
        println("Spring context loaded with Testcontainers PostgreSQL!")
    }
}

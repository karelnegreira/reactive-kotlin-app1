package com.example.springreactive4

import io.r2dbc.postgresql.PostgresqlConnectionConfiguration
import io.r2dbc.postgresql.PostgresqlConnectionFactory
import io.r2dbc.spi.ConnectionFactory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.reactive.awaitSingle
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Configuration
import org.springframework.data.r2dbc.config.AbstractR2dbcConfiguration
import org.springframework.data.r2dbc.core.*
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories
import org.springframework.data.relational.core.mapping.Table
import org.springframework.stereotype.Component
import org.springframework.web.bind.annotation.*

@SpringBootApplication
class SpringReactive4Application

fun main(args: Array<String>) {
	runApplication<SpringReactive4Application>(*args)
}


@Component
class UserRepository(private val client: DatabaseClient) {

	suspend fun count(): Long =
			client.execute("SELECT COUNT(*) FROM users")
					.asType<Long>().fetch().awaitOne()

	fun findAll(): Flow<Users> =
			client.select().from("users").asType<Users>().fetch().flow()

	suspend fun findOne(id: Int): Users? =
			client.execute("SELECT * FROM users WHERE id = $1")
					.bind(0, id).asType<Users>()
					.fetch()
					.awaitOneOrNull()

	suspend fun deleteAll() =
			client.execute("DELETE FROM users")
					.fetch()
					.rowsUpdated()
					.awaitSingle()

	suspend fun save(users: Users) =
			client.insert()
					.into<Users>()
					.table("users")
					.using(users)
					.await()
}

@RestController
@RequestMapping("users")
class RestUserController(private val repository: UserRepository) {
	@GetMapping("/all")
	fun findAll(): Flow<Users> = repository.findAll()

	@GetMapping("/{id}")
	suspend fun findById(@PathVariable("id") id: Int): Users? =
			repository.findOne(id)

	@PostMapping("/save")
	suspend fun save(users: Users) = repository.save(users)

}

@Table("users")
data class Users(val id: Int,
				 val firstname: String,
				 val lastname: String)

@Configuration
@EnableR2dbcRepositories
class PostgresDbConfiguration: AbstractR2dbcConfiguration() {
	override fun connectionFactory(): ConnectionFactory {
		return PostgresqlConnectionFactory(
				PostgresqlConnectionConfiguration.builder()
						.host("localhost")
						.database("test2")
						.username("postgres")
						.password("admin")
						.build()
		)
	}

}

package kicks.db

import cats.effect.IO
import org.flywaydb.core.Flyway

object DbMigrations {
  def run(jdbcUrl: String, username: Option[String] = None, password: Option[String] = None): IO[Unit] = IO.blocking {
    val flyway = Flyway.configure()
      .dataSource(jdbcUrl, username.orNull, password.orNull)
      .locations("classpath:migrations")
      .failOnMissingLocations(true)
      .load()

    flyway.migrate(): Unit
  }

}

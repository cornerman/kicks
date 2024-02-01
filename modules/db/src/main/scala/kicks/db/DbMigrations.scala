package kicks.db

import cats.effect.IO
import org.flywaydb.core.Flyway

object DbMigrations {

  def migrate(jdbcUrl: String): IO[Unit] = IO.blocking {
    val flyway = defaultFlyway(jdbcUrl).load()
    flyway.migrate()
  }.void

  def repair(jdbcUrl: String): IO[Unit] = IO.blocking {
    val flyway = defaultFlyway(jdbcUrl).load()
    flyway.repair()
  }.void

  private def defaultFlyway(jdbcUrl: String) =
    Flyway.configure().dataSource(jdbcUrl, null, null).locations("classpath:migrations").failOnMissingLocations(true)
}
package kicks.db

import cats.effect.IO
import org.flywaydb.core.Flyway

import javax.sql.DataSource

object DbMigrations {

  def migrate(dataSource: DataSource): IO[Unit] = IO.blocking {
    val flyway = defaultFlyway(dataSource).load()
    val _      = flyway.migrate()
  }

  def repair(dataSource: DataSource): IO[Unit] = IO.blocking {
    val flyway = defaultFlyway(dataSource).load()
    val _      = flyway.repair()
  }

  private def defaultFlyway(dataSource: DataSource) =
    Flyway.configure().dataSource(dataSource).locations("classpath:migrations").failOnMissingLocations(true)
}

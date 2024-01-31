package kicks.db

import cats.effect.IO
import org.flywaydb.core.Flyway

object DbMigrations {
  def run(jdbcUrl: String, repair: Boolean = false): IO[Unit] = IO.blocking {
    val flyway = Flyway.configure().dataSource(jdbcUrl, null, null).locations("classpath:migrations").failOnMissingLocations(true).load()

    if (repair) {
      val _ = flyway.repair()
    }

    val _ = flyway.migrate()
  }

}

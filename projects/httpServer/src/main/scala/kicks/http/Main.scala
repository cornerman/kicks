package kicks.http

import cats.implicits._
import cats.effect.{ExitCode, IO, IOApp}
import kicks.db.DbMigrations

object Main extends IOApp {
  override def run(args: List[String]): IO[ExitCode] = {
    val runMode = args.headOption
    val jdbcUrl = System.getenv("DATABASE_URL")

    val state = AppState.create(jdbcUrl = jdbcUrl)

    val runMigrations = DbMigrations.run(jdbcUrl = jdbcUrl)
    val startServer = Server.start(state)

    runMode match {
      case Some("run") => startServer.as(ExitCode.Error)
      case Some("migrate") => runMigrations.as(ExitCode.Error)
      case Some("migrate-and-run") | None => runMigrations *> startServer.as(ExitCode.Error)
      case Some(mode) => IO.println(s"Unknown mode: $mode. Expected: run, migrate, migrate-and-run.").as(ExitCode.Error)
    }

  }
}

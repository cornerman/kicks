package kicks.http

import cats.effect.{ExitCode, IO, IOApp}
import cats.implicits.*
import kicks.db.DbMigrations
import org.http4s.ember.client.EmberClientBuilder

object Main extends IOApp {
  override def run(args: List[String]): IO[ExitCode] = {
    val runMode = args.headOption
    val jdbcUrl = System.getenv("DATABASE_URL")

    val state = AppState.create(jdbcUrl = jdbcUrl)

    val runMigrations = DbMigrations.run(jdbcUrl = jdbcUrl)
    val startServer   = Server.start(state)

    val program = runMode match {
      case Some("run")                    => startServer
      case Some("migrate")                => runMigrations
      case Some("migrate-and-run") | None => runMigrations *> startServer
      case Some(mode)                     => IO.raiseError(new Exception(s"Unknown mode: $mode. Expected: run, migrate, migrate-and-run."))
    }

    program.as(ExitCode.Success)
  }
}

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

//    DbEvents.setup(jdbcUrl)

    val runMigrations = DbMigrations.run(jdbcUrl = jdbcUrl)
    val startServer   = Server.start(state)

    val runner = EmberClientBuilder.default[IO].build.use { client =>
      LitefsEventListener.listen(client).evalTap(IO.println).compile.drain
    }

    println(runMode)

    val program = runMode match {
      case Some("run")                    => startServer.as(ExitCode.Success)
      case Some("migrate")                => runMigrations.as(ExitCode.Success)
      case Some("migrate-and-run") | None => (runMigrations *> startServer).as(ExitCode.Success)
      case Some(mode)                     => IO.println(s"Unknown mode: $mode. Expected: run, migrate, migrate-and-run.").as(ExitCode.Error)
    }

    runner.attempt.map(println(_)) *> program
  }
}

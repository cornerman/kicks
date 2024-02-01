package kicks.http

import cps.*
import cps.monads.catsEffect.{*, given}
import cps.syntax.unary_!
import cats.effect.{ExitCode, IO, IOApp}
import cats.implicits.*
import kicks.db.DbMigrations
import org.http4s.ember.client.EmberClientBuilder

object Main extends IOApp {
  enum Mode {
    case Run, Migrate, Repair
  }

  override def run(args: List[String]): IO[ExitCode] = asyncScope[IO] {
    val modes = args.map(Mode.valueOf).toSet

    val config = AppConfig.fromEnvOrThrow()

    if (modes(Mode.Repair)) {
      !DbMigrations.repair(config.jdbcUrl)
    }

    if (modes(Mode.Migrate) || modes.isEmpty) {
      !DbMigrations.migrate(config.jdbcUrl)
    }

    if (modes(Mode.Run) || modes.isEmpty) {
      val client = !EmberClientBuilder.default[IO].build
      val state  = AppState.create(config, client)
      !Server.start(state)
      !IO.never
    }

    ExitCode.Success
  }
}

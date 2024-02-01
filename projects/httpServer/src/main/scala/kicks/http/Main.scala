package kicks.http

import cps.*
import cps.monads.catsEffect.{*, given}
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

    val config = AppConfig.fromEnv()

    if (modes(Mode.Repair)) {
      await(DbMigrations.repair(config.jdbcUrl))
    }

    if (modes.isEmpty || modes(Mode.Run)) {
      await(DbMigrations.migrate(config.jdbcUrl))
    }

    if (modes.isEmpty || modes(Mode.Migrate)) {
      val client = await(EmberClientBuilder.default[IO].build)
      val state  = AppState.create(config, client)
      await(Server.start(state).useForever)
    }

    ExitCode.Success
  }
}

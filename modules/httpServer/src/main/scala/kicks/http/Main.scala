package kicks.http

import cps.*
import cps.monads.catsEffect.{*, given}
import cps.syntax.unary_!
import cats.effect.{ExitCode, IO, IOApp}
import cats.implicits.*
import kicks.db.DbMigrations
import org.http4s.ember.client.EmberClientBuilder

object Main extends IOApp {
  enum Mode { case Server, Migrate, Repair }

  override def run(args: List[String]): IO[ExitCode] = asyncScope[IO] {
    Logging.setup()

    val config = ServerConfig.fromEnvOrThrow()
    scribe.info(s"Starting application ('${sbt.BuildInfo.version}'): $config")

    val modes = args.map(Mode.valueOf).toSet

    val state = !ServerState.create(config)

    if (modes(Mode.Repair)) {
      !DbMigrations.repair(state.dataSource)
    }

    if (modes(Mode.Migrate) || modes.isEmpty) {
      !DbMigrations.migrate(state.dataSource)
    }

    if (modes(Mode.Server) || modes.isEmpty) {
      !Server.start(state)
      !IO.never
    }

    ExitCode.Success
  }
}

package kicks.http

import cats.effect.{ExitCode, IO, IOApp}
import cats.implicits.*
import kicks.db.DbMigrations

object Main extends IOApp {
  private enum Mode {
    case Run, Migrate, Repair
  }

  override def run(args: List[String]): IO[ExitCode] = {
    val modes = args.map(Mode.valueOf).toSet

    val state = AppState.create(AppStateConfig.fromEnv())

    val shouldRun     = modes.isEmpty || modes(Mode.Run)
    val shouldMigrate = modes.isEmpty || modes(Mode.Migrate)
    val shouldRepair  = modes(Mode.Repair)

    val runMigrations = DbMigrations.run(jdbcUrl = state.config.jdbcUrl, repair = shouldRepair)
    val startServer   = Server.start(state).useForever

    val program =
      runMigrations.whenA(shouldMigrate) *>
        startServer.whenA(shouldRun)

    program.as(ExitCode.Success)
  }
}

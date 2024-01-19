package kicks.http

import cats.effect.{IO, IOApp}

object Main extends IOApp.Simple {
  override def run: IO[Unit] = {
    val state = AppState.create(
      connectionString = "jdbc:sqlite:/home/cornerman/projects/kicks/kicks.db"
    )

    Server.start(state)
  }
}

package kicks.http

import cats.effect.{IO, IOApp}

object Main extends IOApp.Simple {
  override def run: IO[Unit] = {
    val state = AppState.create(
      connectionString = System.getenv("DATABASE_URL")
    )

    Server.start(state)
  }
}

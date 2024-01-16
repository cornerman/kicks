package kicks.http

import cats.effect.{IO, IOApp}

object Main extends IOApp.Simple {
  override def run: IO[Unit] = Server.run
}

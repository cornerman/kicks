package kicks.http

import cats.implicits.*
import cats.effect.IO
import com.comcast.ip4s.*
import cps.macros.Async
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.middleware.Logger
import cps.reify

import scala.concurrent.duration.DurationInt

object Server {
  def start(state: AppState) = for {
    routes <- ServerRoutes.all(state)
    httpApp = Logger.httpApp(true, true)(routes.orNotFound)

    _ <- EmberServerBuilder
           .default[IO]
           .withHost(ipv4"0.0.0.0")
           .withPort(port"8080")
           .withHttpApp(httpApp)
           .withShutdownTimeout(1.seconds)
           .build
           .void
           .useForever
  } yield ()
}

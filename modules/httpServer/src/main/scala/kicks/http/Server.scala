package kicks.http

import cps.*
import cps.syntax.unary_!
import cps.monads.catsEffect.given

import cats.effect.{IO, ResourceIO}
import com.comcast.ip4s.*
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.middleware.Logger

import scala.concurrent.duration.DurationInt

object Server {
  def start(state: ServerState): ResourceIO[Unit] = async[ResourceIO] {
    val routes = !ServerRoutes.all(state)

    val _ = !EmberServerBuilder
      .default[IO]
      .withHost(ipv4"0.0.0.0")
      .withPort(port"8080")
      .withHttpApp(Logger.httpApp(true, true)(routes.orNotFound))
      .withShutdownTimeout(1.seconds)
      .build
  }
}

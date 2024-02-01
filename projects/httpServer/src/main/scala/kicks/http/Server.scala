package kicks.http

import cps.*
import cps.monads.catsEffect.given

import cats.effect.{IO, Resource}
import com.comcast.ip4s.*
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.middleware.Logger

import scala.concurrent.duration.DurationInt

object Server {
  def start(state: AppState): Resource[IO, Unit] = async[Resource[IO, *]] {
    val routes = await(ServerRoutes.all(state))

    val _ = await(
      EmberServerBuilder
        .default[IO]
        .withHost(ipv4"0.0.0.0")
        .withPort(port"8080")
        .withHttpApp(Logger.httpApp(true, true)(routes.orNotFound))
        .withShutdownTimeout(1.seconds)
        .build
    )
  }
}

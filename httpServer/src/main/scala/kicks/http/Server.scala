package kicks.http

import cats.effect.{IO, Resource}
import cats.implicits._
import com.comcast.ip4s._
import org.http4s.{HttpRoutes, ServerSentEvent}
import org.http4s.implicits._
import org.http4s.server.middleware.Logger
import smithy4s.http4s.{swagger, SimpleRestJsonBuilder}
import smithy4s.kicks.{KicksServiceGen, KicksStreamService, KicksStreamServiceGen}
import cats.effect.cps._
import cats.effect.Sync
import cats.implicits._
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl
import org.http4s.ember.server.EmberServerBuilder

import scala.concurrent.duration.DurationInt
import scala.util.Using

object Routes {
  private val dsl = Http4sDsl[IO]
  import dsl._

  private def customRoutes: HttpRoutes[IO] = {
    HttpRoutes.of[IO] { case GET -> Root / "subscribe" / name =>
      val serverSentEvents = KicksEventsImpl.subscribe(name).map { event =>
        ServerSentEvent(data = Some(event.toString))
      }

      Ok(serverSentEvents)
    }
  }

  val all = for {
    kicksRoutes    <- SimpleRestJsonBuilder.routes(KicksServiceImpl.transform(ServiceTypes.singleTransform)).make
    kicksDocsRoutes = swagger.docs[IO](KicksServiceGen)
  } yield kicksRoutes <+> kicksDocsRoutes <+> customRoutes
}

object Server {
  def run: IO[Unit] = async[IO] {
    val routes  = Routes.all.liftTo[IO].await
    val httpApp = Logger.httpApp(true, true)(routes.orNotFound)

    EmberServerBuilder
      .default[IO]
      .withHost(ipv4"0.0.0.0")
      .withPort(port"8080")
      .withHttpApp(httpApp)
      .build
      .useForever
      .await
  }
}

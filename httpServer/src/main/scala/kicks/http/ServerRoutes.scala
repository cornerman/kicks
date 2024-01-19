package kicks.http

import cats.data.Kleisli
import cats.implicits._
import cats.effect.IO
import org.http4s.dsl.Http4sDsl
import org.http4s.{HttpRoutes, Request, Response, ServerSentEvent}
import smithy4s.UnsupportedProtocolError
import smithy4s.http4s.{swagger, SimpleRestJsonBuilder}
import smithy4s.kicks.KicksServiceGen

object ServerRoutes {
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

  def all(state: AppState) = for {
    kicksRoutes    <- SimpleRestJsonBuilder.routes(new KicksServiceImpl(state).transform(AppTypes.singleTransform)).make.liftTo[IO]
    kicksDocsRoutes = swagger.docs[IO](KicksServiceGen)
  } yield kicksRoutes <+> kicksDocsRoutes <+> customRoutes
}

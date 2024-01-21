package kicks.http

import cats.implicits._
import cats.effect.IO
import org.http4s.dsl.Http4sDsl
import org.http4s.{HttpRoutes, ServerSentEvent}
import smithy4s.http4s.{SimpleRestJsonBuilder, swagger}
import kicks.api.KicksServiceGen

object ServerRoutes {
  private val dsl = Http4sDsl[IO]
  import dsl._

  private def customRoutes(state: AppState): HttpRoutes[IO] = {
    val listener = new DbListener(state)

    HttpRoutes.of[IO] { case GET -> Root / "subscribe" / name =>
      val serverSentEvents = listener.subscribe(name).map { event =>
        ServerSentEvent(data = Some(event.toString))
      }

      Ok(serverSentEvents)
    }
  }

  def all(state: AppState) = {
    val serviceImpl = new KicksServiceImpl(state)

    for {
      kicksRoutes    <- SimpleRestJsonBuilder.routes(serviceImpl.transform(AppTypes.serviceResultTransform)).make.liftTo[IO]
      kicksDocsRoutes = swagger.docs[IO](KicksServiceGen)
    } yield kicksRoutes <+> kicksDocsRoutes <+> customRoutes(state)
  }
}

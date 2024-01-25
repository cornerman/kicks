package kicks.http

import cats.implicits._
import cats.effect.IO
import chameleon.{Deserializer, Serializer}
import org.http4s.dsl.Http4sDsl
import org.http4s.{HttpRoutes, Response, ServerSentEvent}
import smithy4s.http4s.{SimpleRestJsonBuilder, swagger}
import kicks.api.KicksServiceGen
import fs2.Stream
import sloth.{Request, RequestPath, Router, ServerFailure}

object ServerRoutes {
  private val dsl = Http4sDsl[IO]
  import dsl._

  private def customRoutes(state: AppState): HttpRoutes[IO] = {
    val listener = new DbListener(state)

    implicit val serializer: Serializer[String, String] = x => x
    implicit val deserializer: Deserializer[String, String] = x => Right(x)

    val requestRouter = Router[String, IO].route(state.requestApi)
    val eventRouter = Router[String, Stream[IO, *]].route(state.eventApi)

    def serverFailureToResponse[F[_]]: ServerFailure => IO[Response[IO]] = {
      case ServerFailure.PathNotFound(_) => NotFound()
      case ServerFailure.HandlerError(err) => InternalServerError(err.getMessage)
      case ServerFailure.DeserializerError(err) => BadRequest(err.getMessage)
    }

    HttpRoutes.of[IO] {
      case request @ _ -> Root / apiName / methodName =>
        val requestPath = RequestPath(apiName, methodName)
        (requestRouter.getFunction(requestPath), eventRouter.getFunction(requestPath)) match {
          case (Some(f), _) =>
            request.as[String].flatMap { payload =>
              f(payload) match {
                case Left(error) => serverFailureToResponse(error)
                case Right(response) => Ok(response)
              }
            }
          case (_, Some(f)) =>
            request.params.get("payload") match {
              case Some(payload) => f(payload) match {
                  case Left(error) => serverFailureToResponse(error)
                  case Right(response) => Ok(response.map(r => ServerSentEvent(data = Some(r))))
                }
              case None => BadRequest()
            }
          case (None, None) => NotFound()
        }
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

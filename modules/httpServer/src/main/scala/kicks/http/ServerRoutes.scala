package kicks.http

import cps.*
import cps.syntax.unary_!
import cps.monads.catsEffect.given
import kicks.http.auth.AuthUser
import kicks.api.KicksServiceGen
import cats.data.{Kleisli, OptionT}
import cats.effect.{IO, Resource}
import cats.effect.std.Dispatcher
import cats.implicits.{*, given}
import org.http4s.implicits.given
import fs2.Stream
import kicks.http
import authn.backend.{AuthnClient, AuthnClientConfig}
import org.http4s.dsl.Http4sDsl
import org.http4s.headers.{`Content-Type`, Location}
import org.http4s.server.{AuthMiddleware, HttpMiddleware, Router}
import org.http4s.server.staticcontent.{fileService, FileService, MemoryCache}
import org.http4s.{AuthedRoutes, ContextRequest, HttpRoutes, Request, Response, ServerSentEvent}
import smithy4s.{Transformation, UnsupportedProtocolError}
import smithy4s.http4s.{swagger, SimpleRestJsonBuilder}
import org.http4s.*
import org.http4s.syntax.all.*

import scala.tools.scalap.scalax.rules.scalasig.ClassFileParser.method

def fileRoutes(state: AppState) = {
  fileService[IO](
    FileService.Config(
      systemPath = state.config.frontendDistributionPath,
      cacheStrategy = MemoryCache[IO](),
    )
  )
}

object ServerRoutes {
  private val dsl = Http4sDsl[IO]
  import dsl.*

  private val getAuthOptionalUser: Kleisli[OptionT[IO, *], Request[IO], AuthUser] =
    Kleisli(_ => OptionT.liftF(IO(AuthUser.Anon)))
  private val getAuthRequiredUser: Kleisli[OptionT[IO, *], Request[IO], AuthUser.User] =
    getAuthOptionalUser.collect { case user: AuthUser.User => user }

  private val middlewareOptionalUser: AuthMiddleware[IO, AuthUser]      = AuthMiddleware(getAuthOptionalUser)
  private val middlewareRequiredUser: AuthMiddleware[IO, AuthUser.User] = AuthMiddleware(getAuthRequiredUser)

  private def rpcRoutes(state: AppState): HttpRoutes[IO] = {
    import chameleon.{Deserializer, Serializer}
    import sloth.{RequestPath, Router, ServerFailure}

    implicit val serializer: Serializer[String, String]     = x => x
    implicit val deserializer: Deserializer[String, String] = x => Right(x)

    val requestRouter = Router[String, IO].route(state.rpc)
    val eventRouter   = Router[String, Stream[IO, *]].route(state.eventRpc)

    def serverFailureToResponse[F[_]]: ServerFailure => IO[Response[IO]] = {
      case ServerFailure.PathNotFound(_)        => NotFound()
      case ServerFailure.HandlerError(err)      => InternalServerError(err.getMessage)
      case ServerFailure.DeserializerError(err) => BadRequest(err.getMessage)
    }

    HttpRoutes.of[IO] { case request @ _ -> Root / apiName / methodName =>
      val requestPath = RequestPath(apiName, methodName)
      (requestRouter.getFunction(requestPath), eventRouter.getFunction(requestPath)) match {
        case (Some(f), _) =>
          request.as[String].flatMap { payload =>
            f(payload) match {
              case Left(error)     => serverFailureToResponse(error)
              case Right(response) => Ok(response)
            }
          }
        case (_, Some(f)) =>
          request.params.get("payload") match {
            case Some(payload) => f(payload) match {
                case Left(error)     => serverFailureToResponse(error)
                case Right(response) => Ok(response.map(r => ServerSentEvent(data = Some(r))))
              }
            case None => BadRequest()
          }
        case (None, None) => NotFound()
      }
    }
  }

  def all(state: AppState): Resource[IO, HttpRoutes[IO]] = reify[Resource[IO, *]] {
    val serviceImplUnified = state.api.transform(AppTypes.serviceResultTransform)(Transformation.service_absorbError_transformation)

    val kicksRoutes     = !SimpleRestJsonBuilder.routes(serviceImplUnified).resource
    val kicksDocsRoutes = swagger.docs[IO](KicksServiceGen)

    fileRoutes(state) <+> kicksRoutes <+> kicksDocsRoutes <+> rpcRoutes(state)
  }
}

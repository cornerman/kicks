package kicks.http

import cats.data.{Kleisli, OptionT}
import cats.effect.{IO, ResourceIO}
import cats.implicits.given
import cats.~>
import fs2.Stream
import kicks.api.KicksServiceGen
import kicks.http.auth.AuthUser
import kicks.http.impl.{ApiImpl, EventRpcImpl, RpcImpl}
import kicks.rpc.{EventRpc, Rpc}
import kicks.shared.{AppConfig, JsonPickler}
import kicks.shared.JsonPickler.*
import chameleon.ext.http4s.JsonStringCodec.*
import org.http4s.*
import org.http4s.dsl.Http4sDsl
import org.http4s.headers.`Content-Type`
import org.http4s.server.staticcontent.{fileService, FileService, MemoryCache}
import org.http4s.server.{AuthMiddleware, Router}
import smithy4s.Transformation
import smithy4s.http4s.{swagger, SimpleRestJsonBuilder}

import java.nio.charset.StandardCharsets
import scala.annotation.unused

object ServerRoutes {
  private val dsl = Http4sDsl[IO]
  import dsl.*

  private val getAuthOptionalUser: Kleisli[OptionT[IO, *], Request[IO], AuthUser] =
    Kleisli(_ => OptionT.liftF(IO(AuthUser.Anon)))
  private val getAuthRequiredUser: Kleisli[OptionT[IO, *], Request[IO], AuthUser.User] =
    getAuthOptionalUser.collect { case user: AuthUser.User => user }

  @unused
  private val middlewareOptionalUser: AuthMiddleware[IO, AuthUser] = AuthMiddleware(getAuthOptionalUser)
  @unused
  private val middlewareRequiredUser: AuthMiddleware[IO, AuthUser.User] = AuthMiddleware(getAuthRequiredUser)

  private def rpcRoutes(state: ServerState): HttpRoutes[IO] = {
    import sloth.{Router, ServerFailure}

    val requestRouter = Router[String, IO].route[Rpc[IO]](RpcImpl(state))
    val eventRouter   = Router[String, Stream[IO, *]].route[EventRpc[Stream[IO, *]]](EventRpcImpl)

//    HttpSloth[IO](requestRouter) <+>
//      HttpSloth[IO].streamRoutes(requestRouter)

    def serverFailureToResponse[F[_]]: ServerFailure => IO[Response[IO]] = {
      case ServerFailure.PathNotFound(_)        => NotFound()
      case ServerFailure.HandlerError(err)      => InternalServerError(err.getMessage)
      case ServerFailure.DeserializerError(err) => BadRequest(err.getMessage)
    }

    HttpRoutes[IO] { case request @ _ -> Root / apiName / methodName =>
      val path = List(apiName, methodName)
      val result = Option((requestRouter.getFunction(path), eventRouter.getFunction(path))).traverseCollect {
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
      }

      OptionT(result)
    }
  }

  private def fileRoutes(state: ServerState): HttpRoutes[IO] =
    fileService[IO](
      FileService.Config(
        systemPath = state.config.frontendDistributionPath,
        cacheStrategy = MemoryCache[IO](),
      )
    )

  private def infoRoutes(state: ServerState): HttpRoutes[IO] =
    HttpRoutes.of[IO] {
      case GET -> Root / "info" / "version"         => Ok(sbt.BuildInfo.version)
      case GET -> Root / "info" / "app_config.json" => Ok(json(state.config.appConfig))
      case req @ (POST -> Root / "info" / "test")   => Ok(jsonAs[AppConfig](req).map(_.toString))
      case GET -> Root / "info" / "app_config.js" =>
        val code = s"window.${AppConfig.domWindowProperty} = ${JsonPickler.write(state.config.appConfig)};"
        Ok(code, `Content-Type`(MediaType.application.`javascript`))
    }

  def apiRoutes(state: ServerState): HttpRoutes[IO] = {
    val apiImplIOError = new ApiImpl(state)
    val absorbIOError = new Transformation.AbsorbError[[E, A] =>> IO[Either[E, A]], IO] {
      def apply[E, A](fa: IO[Either[E, A]], injectError: E => Throwable): IO[A] = fa.map(_.leftMap(injectError)).rethrow
    }
    val apiImplIO = apiImplIOError.transform(absorbIOError)(Transformation.service_absorbError_transformation)

    val apiRoutes     = SimpleRestJsonBuilder.routes(apiImplIO).make.getOrElse(HttpRoutes.of[IO](PartialFunction.empty))
    val apiDocsRoutes = swagger.docs[IO](KicksServiceGen)

    apiRoutes <+> apiDocsRoutes
  }

  def all(state: ServerState): HttpRoutes[IO] =
    fileRoutes(state) <+> infoRoutes(state) <+> apiRoutes(state) <+> rpcRoutes(state)
}

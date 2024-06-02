package kicks.http

import cats.data.{Kleisli, OptionT}
import cats.effect.{IO, Resource}
import cats.implicits.given
import com.github.plokhotnyuk.jsoniter_scala.core.writeToString
import cps.*
import cps.monads.catsEffect.given
import cps.syntax.unary_!
import fs2.Stream
import kicks.api.KicksServiceGen
import kicks.http.auth.AuthUser
import kicks.http.impl.{ApiImpl, EventRpcImpl, RpcImpl}
import kicks.shared.AppConfig
import org.http4s.dsl.Http4sDsl
import org.http4s.server.{AuthMiddleware, Router}
import org.http4s.server.staticcontent.{fileService, FileService, MemoryCache}
import org.http4s.*
import smithy4s.Transformation
import smithy4s.http4s.{swagger, SimpleRestJsonBuilder}
import http4sJsoniter.ArrayEntityCodec.*
import org.http4s.headers.`Content-Type`
import org.sqlite.SQLiteDataSource

import scala.annotation.unused
import scala.util.chaining.*

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
    import chameleon.{Deserializer, Serializer}
    import sloth.{RequestPath, Router, ServerFailure}

    implicit val serializer: Serializer[String, String]     = x => x
    implicit val deserializer: Deserializer[String, String] = x => Right(x)

    val requestRouter = Router[String, IO].route(RpcImpl)
    val eventRouter   = Router[String, Stream[IO, *]].route(EventRpcImpl)

    def serverFailureToResponse[F[_]]: ServerFailure => IO[Response[IO]] = {
      case ServerFailure.PathNotFound(_)        => NotFound()
      case ServerFailure.HandlerError(err)      => InternalServerError(err.getMessage)
      case ServerFailure.DeserializerError(err) => BadRequest(err.getMessage)
    }

    HttpRoutes[IO] { case request @ _ -> Root / apiName / methodName =>
      val path = RequestPath(apiName, methodName)
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

  private def infoRoutes(state: ServerState): HttpRoutes[IO] = {
    def appConfig = AppConfig(
      authnUrl = state.config.authnIssuerUrl
    )

    HttpRoutes.of[IO] {
      case GET -> Root / "info" / "version"         => Ok(sbt.BuildInfo.version)
      case GET -> Root / "info" / "app_config.json" => Ok(appConfig)
      case GET -> Root / "info" / "app_config.js" =>
        val code = s"window.${AppConfig.domWindowProperty} = ${writeToString(appConfig)};"
        Ok(code, `Content-Type`(MediaType.application.`javascript`))
    }
  }

  def all(state: ServerState): Resource[IO, HttpRoutes[IO]] = async[Resource[IO, *]] {
    val apiImpl = new ApiImpl(state.xa, state.dataSource)
    val apiImplF = apiImpl.transform(new Transformation.AbsorbError[[E, A] =>> IO[Either[E, A]], IO] {
      def apply[E, A](fa: IO[Either[E, A]], injectError: E => Throwable): IO[A] = fa.map(_.leftMap(injectError)).rethrow
    })(Transformation.service_absorbError_transformation)

    val apiRoutes     = !SimpleRestJsonBuilder.routes(apiImplF).resource
    val apiDocsRoutes = swagger.docs[IO](KicksServiceGen)

    fileRoutes(state) <+>
      infoRoutes(state) <+>
      apiRoutes <+>
      apiDocsRoutes <+>
      rpcRoutes(state)
  }
}

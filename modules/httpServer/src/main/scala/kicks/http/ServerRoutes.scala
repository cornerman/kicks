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
import org.http4s.*
import org.http4s.dsl.Http4sDsl
import org.http4s.headers.`Content-Type`
import org.http4s.server.staticcontent.{fileService, FileService, MemoryCache}
import org.http4s.server.{AuthMiddleware, Router}
import smithy4s.Transformation
import smithy4s.http4s.{swagger, SimpleRestJsonBuilder}
import sloth.ext.http4s.server.HttpRpcRoutes

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

  private def fileRoutes(state: ServerState): HttpRoutes[IO] =
    fileService[IO](
      FileService.Config(
        systemPath = state.config.frontendDistributionPath,
        cacheStrategy = MemoryCache[IO](),
      )
    )

  private def infoRoutes(state: ServerState): HttpRoutes[IO] =
    HttpRoutes.of[IO] {
      case GET -> Root / "info" / "version" =>
        Ok(sbt.BuildInfo.version)
      case GET -> Root / "info" / "app_config.json" =>
        val json = JsonPickler.write(state.config.appConfig)
        Ok(json, `Content-Type`(MediaType.application.`javascript`))
      case GET -> Root / "info" / "app_config.js" =>
        val code = s"window.${AppConfig.domWindowProperty} = ${JsonPickler.write(state.config.appConfig)};"
        Ok(code, `Content-Type`(MediaType.application.`javascript`))
    }

  private def rpcRoutes(state: ServerState): HttpRoutes[IO] = {
    val requestRouter = sloth.Router[String, IO].route[Rpc[IO]](RpcImpl(state))
    val eventRouter   = sloth.Router[String, Stream[IO, *]].route[EventRpc[Stream[IO, *]]](EventRpcImpl)

    HttpRpcRoutes.apply[String, IO](requestRouter) <+> HttpRpcRoutes.eventStream[IO](eventRouter)
  }

  private def apiRoutes(state: ServerState): HttpRoutes[IO] = {
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

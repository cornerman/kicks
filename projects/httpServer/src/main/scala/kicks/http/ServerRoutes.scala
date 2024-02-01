package kicks.http

import kicks.http.auth.{AuthUser, Pac4jConfig}
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
import org.pac4j.core.profile.CommonProfile
import smithy4s.{Transformation, UnsupportedProtocolError}
import smithy4s.http4s.{swagger, SimpleRestJsonBuilder}
import org.http4s.*
import org.http4s.syntax.all.*

import scala.tools.scalap.scalax.rules.scalasig.ClassFileParser.method

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

    val requestRouter = Router[String, IO].route(state.requestApi)
    val eventRouter   = Router[String, Stream[IO, *]].route(state.eventApi)

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

  private def authManagementRoutes(dispatcher: Dispatcher[IO]): (HttpMiddleware[IO], HttpRoutes[IO]) = {
    import org.pac4j.http4s._

    val contextBuilder = Http4sWebContext.withDispatcherInstance(dispatcher)

    val clientConfig =
      Pac4jConfig.clientConfig(callbackUrl = "http://localhost:8080/callback", loginFormUrl = "http://localhost:8080/loginForm")
    val sessionConfig = Pac4jConfig.sessionConfig

    val callbackService = new CallbackService[IO](clientConfig, contextBuilder)
    val logoutService   = new LogoutService[IO](clientConfig, contextBuilder, Some("/?afterlogout"), destroySession = true)

    val authedRedirect: AuthedRoutes[List[CommonProfile], IO] = Kleisli(_ => OptionT.liftF(Found(Location(uri"/"))))
    def loginService(clientName: String): HttpRoutes[IO] =
      SecurityFilterMiddleware.securityFilter[IO](clientConfig, contextBuilder, Some(clientName)).apply(authedRedirect)

    val loginRoutes = Router(
      "login" -> Router(
        "form" -> loginService("FormClient")
      )
    )

    val interactionRoutes = HttpRoutes.of[IO] {
      case GET -> Root / "loginForm" =>
        Ok(
          """
            |<form action="http://localhost:8080/callback?client_name=FormClient" method="POST">
            |    <input type="text" name="username" value="">
            |    <p></p>
            |    <input type="password" name="password" value="">
            |    <p></p>
            |    <input type="submit" name="submit" value="Submit">
            |</form>
            |""".stripMargin.stripLeading
        ).map(_.withContentType(`Content-Type`(MediaType.text.html)))
      case req @ (GET | POST) -> Root / "callback" => callbackService.callback(req)
      case req @ (GET | POST) -> Root / "logout"   => logoutService.logout(req)
    }

    val authedRoutes = AuthedRoutes.of[List[CommonProfile], IO] { case req @ (GET | POST) -> Root / "foobar" as profiles =>
      Ok(profiles.mkString(", "))

    }

    Session.sessionManagement[IO](sessionConfig) -> (interactionRoutes <+> loginRoutes <+> Session
      .sessionManagement[IO](sessionConfig)
      .compose(SecurityFilterMiddleware.securityFilter[IO](clientConfig, contextBuilder))
      .apply(authedRoutes))
  }

  def all(state: AppState): Resource[IO, HttpRoutes[IO]] = {
    val serviceImpl        = new KicksServiceImpl(state)
    val serviceImplUnified = serviceImpl.transform(AppTypes.serviceResultTransform)(Transformation.service_absorbError_transformation)

    val staticFiles = fileService[IO](
      FileService.Config(
        systemPath = state.config.frontendDistributionPath,
        cacheStrategy = MemoryCache[IO](),
      )
    )

    for {
      kicksRoutes                    <- SimpleRestJsonBuilder.routes(serviceImplUnified).resource
      dispatcher                     <- Dispatcher.parallel[IO]
      kicksDocsRoutes                 = swagger.docs[IO](KicksServiceGen)
      (sessionManagement, authRoutes) = authManagementRoutes(dispatcher)
    } yield staticFiles <+> kicksRoutes <+> kicksDocsRoutes <+> authRoutes <+> rpcRoutes(state)
  }
}

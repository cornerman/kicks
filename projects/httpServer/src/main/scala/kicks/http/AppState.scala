package kicks.http

import authn.backend.{AuthnClient, AuthnClientConfig}
import cats.effect.IO
import doobie.util.transactor.Transactor
import fs2.Stream
import kicks.rpc.{EventRpc, RequestRpc}
import org.http4s.client.Client

import scala.concurrent.duration.DurationInt

case class AppConfig(
  jdbcUrl: String,
  frontendDistributionPath: String,
  authnIssuerUrl: String,
  authnAdminUrl: String,
  authnAdminUsername: String,
  authnAdminPassword: String,
)
object AppConfig {
  def fromEnv() = AppConfig(
    jdbcUrl = sys.env("DATABASE_URL"),
    frontendDistributionPath = sys.env("FRONTEND_DISTRIBUTION_PATH"),
    authnIssuerUrl = sys.env("AUTHN_ISSUER_URL"),
    authnAdminUrl = sys.env("AUTHN_ADMIN_URL"),
    authnAdminUsername = sys.env("AUTHN_ADMIN_USERNAME"),
    authnAdminPassword = sys.env("AUTHN_ADMIN_PASSWORD"),
  )
}

case class AppState(
  xa: Transactor[IO],
  authn: AuthnClient[IO],
  requestApi: RequestRpc[IO],
  eventApi: EventRpc[Stream[IO, *]],
  config: AppConfig,
)
object AppState {
  def create(config: AppConfig, client: Client[IO]): AppState = {
    AppState(
      xa = Transactor.fromDriverManager[IO](classOf[org.sqlite.JDBC].getName, config.jdbcUrl, None /*, Some(LogHandler.jdkLogHandler)*/ ),
      authn = AuthnClient[IO](
        AuthnClientConfig(
          issuer = config.authnIssuerUrl,
          adminURL = Some(config.authnAdminUrl),
          username = config.authnAdminUsername,
          password = config.authnAdminPassword,
          audiences = Set("kicks"),
        ),
        client,
      ),
      requestApi = new RequestRpc[IO] {
        override def foo(s: String): IO[String] = IO("Hej " + s)
      },
      eventApi = new EventRpc[Stream[IO, *]] {
        override def foo(s: String): Stream[IO, String] = Stream("Hallo", "du", "ei", s).evalTap(_ => IO.sleep(1.seconds))
      },
      config = config,
    )
  }
}

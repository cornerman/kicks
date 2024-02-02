package kicks.http

import authn.backend.{AuthnClient, AuthnClientConfig}
import cats.effect.IO
import doobie.util.transactor.Transactor
import org.http4s.client.Client

case class ServerState(
  xa: Transactor[IO],
  authn: AuthnClient[IO],
  config: ServerConfig,
)

object ServerState {
  def create(config: ServerConfig, client: Client[IO]): ServerState = {
    val xa = Transactor.fromDriverManager[IO]("org.sqlite.JDBC", config.jdbcUrl, None)
    ServerState(
      xa = xa,
      authn = AuthnClient[IO](authnConfig(config), client),
      config = config,
    )
  }

  private def authnConfig(config: ServerConfig) = AuthnClientConfig(
    issuer = config.authnIssuerUrl,
    adminURL = Some(config.authnAdminUrl),
    username = config.authnAdminUsername,
    password = config.authnAdminPassword,
    audiences = Set(config.authnAudience),
  )
}

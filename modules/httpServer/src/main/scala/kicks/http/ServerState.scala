package kicks.http

import cps.*
import cps.syntax.unary_!
import cps.monads.catsEffect.given

import authn.backend.{AuthnClient, AuthnClientConfig}
import cats.effect.{IO, ResourceIO}
import org.http4s.client.Client
import org.http4s.ember.client.EmberClientBuilder
import org.sqlite.SQLiteDataSource
import javax.sql.DataSource

import scala.util.chaining.*

case class ServerState(
  dataSource: DataSource,
  authn: AuthnClient[IO],
  config: ServerConfig,
)

object ServerState {
  def create(config: ServerConfig): ResourceIO[ServerState] = async[ResourceIO] {
    val client = !EmberClientBuilder.default[IO].build

    ServerState(
      dataSource = SQLiteDataSource().tap(_.setUrl(config.jdbcUrl)),
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

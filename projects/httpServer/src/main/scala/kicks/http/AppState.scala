package kicks.http

import authn.backend.{AuthnClient, AuthnClientConfig}
import cats.effect.IO
import doobie.util.transactor.Transactor
import fs2.Stream
import kicks.rpc.{EventRpc, Rpc}
import org.http4s.client.Client

import scala.concurrent.duration.DurationInt

case class AppState(
  xa: Transactor[IO],
  authn: AuthnClient[IO],
  config: AppConfig,
) {
  val api = new KicksServiceImpl(this)
  val rpc = new Rpc[IO] {
    override def foo(s: String): IO[String] = IO("Hej " + s)
  }
  val eventRpc = new EventRpc[Stream[IO, *]] {
    override def foo(s: String): Stream[IO, String] = Stream("Hallo", "du", "ei", s).evalTap(_ => IO.sleep(1.seconds))
  }
}

object AppState {
  def create(config: AppConfig, client: Client[IO]): AppState = {
    AppState(
      xa = Transactor.fromDriverManager[IO]("org.sqlite.JDBC", config.jdbcUrl, None),
      authn = AuthnClient[IO](authnConfig(config), client),
      config = config,
    )
  }

  private def authnConfig(config: AppConfig) = AuthnClientConfig(
    issuer = config.authnIssuerUrl,
    adminURL = Some(config.authnAdminUrl),
    username = config.authnAdminUsername,
    password = config.authnAdminPassword,
    audiences = Set("kicks"),
  )
}

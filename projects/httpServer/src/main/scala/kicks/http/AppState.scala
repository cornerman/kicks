package kicks.http

import cats.effect.IO
import doobie.util.transactor.Transactor
import fs2.Stream
import kicks.rpc.{EventRpc, RequestRpc}

import scala.concurrent.duration.DurationInt

case class AppStateConfig(
  jdbcUrl: String,
  frontendDistributionPath: String,
)
object AppStateConfig {
  def fromEnv() = AppStateConfig(
    jdbcUrl = sys.env("DATABASE_URL"),
    frontendDistributionPath = sys.env("FRONTEND_DISTRIBUTION_PATH"),
  )
}

case class AppState(
  xa: Transactor[IO],
  requestApi: RequestRpc[IO],
  eventApi: EventRpc[Stream[IO, *]],
  config: AppStateConfig,
)
object AppState {
  def create(
    config: AppStateConfig
  ): AppState = {
    AppState(
      xa = Transactor.fromDriverManager[IO](classOf[org.sqlite.JDBC].getName, config.jdbcUrl, None /*, Some(LogHandler.jdkLogHandler)*/ ),
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

package kicks.http

import cats.effect.{Async, IO}
import doobie.LogHandler
import doobie.util.transactor.Transactor
import fs2.Stream
import kicks.rpc.{EventRpc, RequestRpc}

import scala.concurrent.duration.DurationInt

case class AppState(
                     xa: Transactor[IO],
                     requestApi: RequestRpc[IO],
                     eventApi: EventRpc[Stream[IO, *]],
)
object AppState {
  def create(jdbcUrl: String): AppState = {
    AppState(
      xa = Transactor.fromDriverManager[IO](classOf[org.sqlite.JDBC].getName, jdbcUrl, Some(LogHandler.jdkLogHandler)),
      requestApi = new RequestRpc[IO] {
        override def foo(s: String): IO[String] = IO("Hej " + s)
      },
      eventApi = new EventRpc[Stream[IO, *]] {
        override def foo(s: String): Stream[IO, String] = Stream("Hallo", "du", "ei", s).evalTap(_ => IO.sleep(1.seconds))
      },
    )
  }
}

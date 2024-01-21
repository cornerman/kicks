package kicks.http

import cats.effect.{Async, IO}
import doobie.LogHandler
import doobie.util.transactor.Transactor

case class AppState(
  xa: Transactor[IO]
)
object AppState {
  def create(jdbcUrl: String): AppState = {
    val xa = Transactor.fromDriverManager[IO](classOf[org.sqlite.JDBC].getName, jdbcUrl, Some(LogHandler.jdkLogHandler))

    AppState(
      xa = xa
    )
  }
}

package kicks.http

import cats.effect.{Async, IO}
import doobie.util.transactor.Transactor

case class AppState(
  xa: Transactor[IO],
                       )
object AppState {
  def create(connectionString: String): AppState = {
    val xa = Transactor.fromDriverManager[IO](classOf[org.sqlite.JDBC].getName, connectionString)

    AppState(
      xa = xa
    )
  }
}

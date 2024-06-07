package kicks.rpc

import kicks.shared.model.*

trait Rpc[F[_]] {
  def foo(s: String): F[String]
  def version: F[String]
  def getPostThread(rootId: Int): F[Option[Thread]]
}

trait EventRpc[F[_]] {
  def foo(s: String): F[String]
}

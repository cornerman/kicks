package kicks.rpc

import kicks.shared.model.*

trait Rpc[F[_]] {
  def foo(s: String): F[String]
  def getPost(id: Int): F[Option[Post]]
}

trait EventRpc[F[_]] {
  def foo(s: String): F[String]
}

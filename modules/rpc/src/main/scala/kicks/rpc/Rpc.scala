package kicks.rpc

import kicks.shared.model.*

trait Rpc[F[_]] {
  def version: F[String]

  def addPost(post: Post.Creator, parentId: Option[Int]): F[Post]
  def getPostThread(rootId: Int): F[Option[Thread]]
}

trait EventRpc[F[_]] {
  def foo(s: String): F[String]
}

package kicks.rpc

trait Rpc[F[_]] {
  def foo(s: String): F[String]
}

trait EventRpc[F[_]] {
  def foo(s: String): F[String]
}

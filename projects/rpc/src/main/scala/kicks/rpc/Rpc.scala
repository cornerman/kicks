package kicks.rpc

trait RequestRpc[F[_]] {
  def foo(s: String): F[String]
}

trait EventRpc[F[_]] {
  def foo(s: String): F[String]
}

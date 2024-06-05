package kicks.http.impl

import kicks.rpc.Rpc
import cats.effect.IO

object RpcImpl extends Rpc[IO] {
  override def foo(s: String): IO[String] = IO("Hej " + s)
}

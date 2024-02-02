package kicks.http.impl

import cats.effect.IO
import fs2.Stream
import kicks.rpc.EventRpc

import scala.concurrent.duration.DurationInt

object EventRpcImpl extends EventRpc[Stream[IO, *]] {
  override def foo(s: String): Stream[IO, String] = Stream("Hallo", "du", "ei", s).evalTap(_ => IO.sleep(1.seconds))
}

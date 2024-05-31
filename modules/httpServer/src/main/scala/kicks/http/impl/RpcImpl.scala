package kicks.http.impl

import cats.effect.IO
import doobie.implicits.*
import fs2.Stream
import kicks.api.{AppError, Greeting, KicksServiceGen}
import kicks.db.Db
import kicks.db.quill.schema.Foo
import kicks.rpc.{EventRpc, Rpc}

object RpcImpl extends Rpc[IO] {
  override def foo(s: String): IO[String] = IO("Hej " + s)
}

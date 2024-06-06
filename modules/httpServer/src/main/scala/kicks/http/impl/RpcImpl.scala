package kicks.http.impl

import com.augustnagro.magnum.*
import kicks.shared.model.*
import kicks.rpc.Rpc
import cats.effect.IO
import kicks.db.{Db, ModelTransform, Queries}
import kicks.db.model.PostRepo
import kicks.http.ServerState

class RpcImpl(state: ServerState) extends Rpc[IO] {
  override def foo(s: String): IO[String] = IO("Hej " + s)

  override def getPost(id: Int): IO[Option[Post]] = IO.blocking {
    val dbPosts = connect(state.dataSource)(Queries.postChain(id).run())
    ModelTransform.fromDb(dbPosts)
  }
}

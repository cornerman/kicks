package kicks.http.impl

import com.augustnagro.magnum.{sql, Spec}
import com.augustnagro.magnum.ce.*
import kicks.shared.model.*
import kicks.rpc.Rpc
import cats.effect.IO
import kicks.db.{Db, ModelRead}
import kicks.db.model.{PostChain, PostChainRepo, PostRepo}
import kicks.http.ServerState

class RpcImpl(state: ServerState) extends Rpc[IO] {
  override def foo(s: String): IO[String] = IO("Hej " + s)

  override def version: IO[String] = IO.pure(sbt.BuildInfo.version)

  override def getPostThread(rootId: Int): IO[Option[Thread]] = connectF[IO](state.dataSource)(lift {
    val dbPost = !PostChainRepo.findAll(
      PostChain.Spec.where(sql"${PostChain.Table.rootPostId} = ${rootId}")
    )

    ModelRead.postChain(dbPost)
  })
}

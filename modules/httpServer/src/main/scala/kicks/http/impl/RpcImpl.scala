package kicks.http.impl

import cats.effect.IO
import com.augustnagro.magnum.ce.*
import com.augustnagro.magnum.{sql, Frag, Spec}
import kicks.db.model.{Post as DbPost, PostChain, PostChainRepo, PostConn, PostConnRepo, PostRepo}
import kicks.db.Db
import kicks.http.{ModelMap, ServerState}
import kicks.rpc.Rpc
import kicks.shared.model.*
import io.github.arainko.ducktape.*

class RpcImpl(state: ServerState) extends Rpc[IO] {
  override def version: IO[String] = IO.pure(sbt.BuildInfo.version)

  override def addPost(post: Post.Creator, parentId: Option[Int]): IO[Post] = transactF[IO](state.dataSource)(lift {
    val dbPost = !PostRepo.insertReturning(post.to[DbPost.Creator])
    parentId.foreach { parentId =>
      !PostConnRepo.insert(PostConn.Creator(parentId = parentId, childId = dbPost.id))
    }
    dbPost.to[Post]
  })

  override def getPostThread(rootId: Int): IO[Option[Thread]] = connectF[IO](state.dataSource)(lift {
    val dbPost = !PostChainRepo.findAll(
      PostChain.Spec.where(sql"${PostChain.Table.rootPostId} = ${rootId}")
    )

    ModelMap.postChain(dbPost)
  })
}

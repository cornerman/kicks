package kicks.http.impl

import cats.effect.IO
import com.augustnagro.magnum.ce.*
import com.augustnagro.magnum.{sql, Frag, Spec}
import kicks.db.Db
import kicks.http.{ModelTransformer, ServerState}
import kicks.rpc.Rpc
import kicks.db.model.*
import kicks.shared.model
import ModelTransformer.given
import io.github.arainko.ducktape.*

class RpcImpl(state: ServerState) extends Rpc[IO] {
  override def version: IO[String] = IO.pure(sbt.BuildInfo.version)

  override def addPost(post: model.Post.Creator, parentId: Option[Int]): IO[model.Post] = transactF[IO](state.dataSource)(lift {
    val dbPost = !PostRepo.insertReturning(post.to[Post.Creator])
    parentId.foreach { parentId =>
      !PostConnRepo.insert(PostConn.Creator(parentId = parentId, childId = dbPost.id))
    }
    dbPost.to[model.Post]
  })

  override def getPostThread(rootId: Int): IO[Option[model.Thread]] = connectF[IO](state.dataSource)(lift {
    val dbPosts = !PostChainRepo.findAll(PostChain.Spec.whereEqRootPostId(Some(rootId)))
    ModelTransformer.postChain(dbPosts)
  })
}

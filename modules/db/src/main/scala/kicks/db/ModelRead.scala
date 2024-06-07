package kicks.db

import kicks.db.model as db
import kicks.shared.model as shared

object ModelRead {
  def postChain(posts: Vector[db.PostChain]): Option[shared.Thread] = {
    val childrenMap = posts.groupBy(_.parentPostId)

    def buildTree(parentId: Option[Int]): Vector[shared.Thread] =
      childrenMap.getOrElse(parentId, Vector.empty).map { post =>
        shared.Thread(
          root = shared.Post(id = post.id.get, text = post.text.get),
          children = buildTree(post.id),
        )
      }

    buildTree(None) match {
      case Vector(root) => Some(root)
      case _            => None
    }
  }
}

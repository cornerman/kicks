package kicks.db

import kicks.db.model as db
import kicks.shared.model as shared

object ModelTransform {
  def fromDb(posts: Vector[db.Post]): Option[shared.Post] = {
    val childrenMap = posts.groupBy(_.parentId)

    def buildTree(parentId: Option[Int], posts: Vector[db.Post]): Vector[shared.Post] =
      childrenMap(parentId).map { post =>
        shared.Post(id = post.id, text = post.text, children = buildTree(Some(post.id), posts))
      }

    buildTree(None, posts) match {
      case Vector(root) => Some(root)
      case _            => None
    }
  }

  def toDb(post: shared.Post, parentId: Option[Int] = None): Vector[db.Post] =
    val dbPost = db.Post(id = post.id, parentId = None, text = post.text)
    dbPost +: post.children.flatMap(toDb(_, Some(post.id)))
}

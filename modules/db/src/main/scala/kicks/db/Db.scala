package kicks.db

import cats.implicits.*
import com.augustnagro.magnum.ce.*
import kicks.db.model.*
import cats.effect.IO
import com.augustnagro.magnum.Frag

import javax.sql.DataSource
Frag()

object Db {
  def run(ds: DataSource): IO[Unit] = transactF(ds)(lift {
    val postCreator = Post.Creator("my test")
    val posts       = !PostRepo.findAll
    val parent      = if (posts.nonEmpty) posts.get(util.Random.nextInt((posts.size * 1.3).toInt)) else None
    val post        = !PostRepo.insertReturning(postCreator)
    parent.foreach { parent =>
      !PostConnRepo.insert(PostConn.Creator(childId = post.id, parentId = parent.id))
    }
    println(s"Created post: $post")
    //    PersonRepo.insert(personCreator)

    // val persons = PersonRepo.findAll
    val allPosts = !PostChainRepo.findAll
    // val persons = PersonRepo.findAll(spec).headOption
    println(s"All posts: ${allPosts.mkString("\n", "\n", "\n")}")
  })
}

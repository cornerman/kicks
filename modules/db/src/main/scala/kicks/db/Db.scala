package kicks.db

import com.augustnagro.magnum.*
import kicks.db.magnum.schema.*

import javax.sql.DataSource

object Db {
  def run(ds: DataSource): Unit = transact(ds) {

    val postCreator = Post.Creator(None, "my test")
    val post        = PostRepo.insertReturning(postCreator)
    println(s"Created post: $post")
//    PersonRepo.insert(personCreator)

    // val persons = PersonRepo.findAll
    val posts = PostRepo.findAll
    // val persons = PersonRepo.findAll(spec).headOption
    println(s"All posts: ${posts.mkString("\n", "\n", "\n")}")
  }
}

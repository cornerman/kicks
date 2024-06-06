package kicks.db

import kicks.db.model.*
import com.augustnagro.magnum.given
import com.augustnagro.magnum.*

object Queries {

  def postChain(id: Int): Query[Post] =
    sql"""
WITH RECURSIVE post_tree(${Post.Table.all}) AS (
 SELECT * FROM post WHERE ${Post.Table.id} = ${id}
 UNION ALL
 SELECT p.* FROM post p INNER JOIN post_tree pt ON p.${Post.Table.parentId} = pt.${Post.Table.id}
)
SELECT ${Post.Table.all} FROM post_tree;
     """.query[Post]
}

package kicks.db

import kicks.db.model.*
import com.augustnagro.magnum.*

object Queries {

  // Usage:
  // import com.augustnagro.magnum.ce.*
  // example(1).runF[IO]
  def example(id: Int): Query[Int] =
    sql"select $id".query[Int]
}

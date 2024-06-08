package kicks.db

import com.augustnagro.magnum.*
import kicks.db.model.*

object Queries {

  // Usage:
  // import com.augustnagro.magnum.ce.*
  // example(1).runF[IO]
  def example(id: Int): Query[Int] =
    sql"select $id".query[Int]
}

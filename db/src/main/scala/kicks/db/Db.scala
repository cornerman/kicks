package kicks.db

import cats.effect.IO
import doobie.ConnectionIO
import doobie.implicits._
import cats.implicits._
import io.getquill.{Literal, SqliteDialect}
import io.getquill.doobie.DoobieContext
import kicks.db.schema._

private object DbContext extends DoobieContext.SQLite(Literal) with SchemaExtensions[SqliteDialect, Literal]

object Db {
  import DbContext.{SqlInfixInterpolator => _, _} // Quill's `sql` interpolator conflicts with doobie so don't import it
  import DbContext.compat._ // Import the qsql interpolator instead

  def fun(person: Foo): ConnectionIO[Unit] = {
    val queryRun: ConnectionIO[Unit] = run(quote { FooDao.query }).map(println(_))
    val insertRun: ConnectionIO[Unit] = run(quote { FooDao.query.insertValue(lift(person)) }).map(println(_))
    (queryRun *> insertRun *> queryRun)
  }
}
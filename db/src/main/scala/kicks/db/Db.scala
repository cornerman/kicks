package kicks.db

import doobie.ConnectionIO
import doobie.implicits._
import cats.implicits._
import io.getquill.{Literal, SqliteDialect}
import io.getquill.doobie.DoobieContext
import kicks.db.schema._

private object DbContext extends DoobieContext.SQLite(Literal) with SchemaExtensions[SqliteDialect, Literal]

object Db {
  import DbContext.{SqlInfixInterpolator => _, _} // Quill's `sql` interpolator conflicts with doobie. Use DbContext.compat._ if needed.

  def fun(person: Foo): ConnectionIO[Unit] = {
    val queryRun: ConnectionIO[Unit]  = run(FooDao.query).map(println(_))
    val insertRun: ConnectionIO[Unit] = run(FooDao.query.insertValue(lift(person))).map(println(_))
    (queryRun *> insertRun *> queryRun)
  }
}

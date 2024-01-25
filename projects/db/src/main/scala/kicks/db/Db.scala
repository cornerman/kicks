package kicks.db

import doobie.ConnectionIO
import doobie.implicits._
import cats.implicits._
import io.getquill.{EntityQuery, Literal, SqliteDialect}
import io.getquill.doobie.DoobieContext
import kicks.db.schema._

private object DbContext extends DoobieContext.SQLite(Literal) with SchemaExtensions

object Db {
  import DbContext._

  def fun(person: Foo): ConnectionIO[Unit] = {
    import io.getquill._
    val queryRun: ConnectionIO[Unit]  = run(quote(FooDao.query)).map(println(_))
    val insertRun: ConnectionIO[Unit] = run(quote(FooDao.query.insertValue(lazyLift(person)))).map(println(_))
    (queryRun *> insertRun *> queryRun)
  }

//  def fun2(person: Foo): ConnectionIO[Unit] = {
//    val queryRun: ConnectionIO[Unit]  = run(dynamicQuerySchema[Foo]("foo")).map(println(_))
//    val insertRun: ConnectionIO[Unit] = run(quote(dynamicQuerySchema[Foo]("foo").insertValue(lift(person)))).map(println(_))
//    (queryRun *> insertRun *> queryRun)
//  }

//  def foo[T](person: T, query: DynamicEntityQuery[T]): ConnectionIO[Unit] = {
//    val queryRun: ConnectionIO[T]  = run(query.q)
//    queryRun.map(println(_))
//  }
}

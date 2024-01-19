package kicks.http

import cats.effect.IO
import cats.effect.cps._
import smithy4s.kicks.{AppError, Greeting, KicksServiceGen}
import kicks.db.Db
import kicks.db.schema.Foo
import doobie.implicits._
import doobie.util.transactor.Transactor

class KicksServiceImpl(state: AppState) extends KicksServiceGen[AppTypes.SingleResponse] {

  val xa = Transactor.fromDriverManager[IO](classOf[org.sqlite.JDBC].getName, "jdbc:sqlite:/home/cornerman/projects/kicks/kicks.db")

  override def hello(name: String, town: Option[String]) = name match {
      case "db" => Db.fun(Foo(Some("foos"))).transact(state.xa).as(Right(Greeting("Done")))
      case "heinz" => IO(Left(KicksServiceGen.HelloError.appError(AppError())))
      case t       => IO(Right(Greeting(s"Hello $name from $t!")))
    }
}
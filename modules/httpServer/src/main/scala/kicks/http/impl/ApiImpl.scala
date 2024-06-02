package kicks.http.impl

import cats.effect.IO
import doobie.implicits.*
import doobie.util.transactor.Transactor
import kicks.api.{AppError, Greeting, KicksServiceGen}
import kicks.db.{Db, Db2}
import kicks.db.quill.schema.Foo

import javax.sql.DataSource

class ApiImpl(xa: Transactor[IO], dataSource: DataSource) extends KicksServiceGen[[_, E, A, _, _] =>> IO[Either[E, A]]] {
  override def hello(name: String, town: Option[String]) = name match {
    case "db"    => Db.fun(Foo(Some("foos"))).transact(xa).as(Right(Greeting("Done")))
    case "db2"   => IO.blocking(Db2.run(dataSource)).onError(x => { x.printStackTrace(); IO.println(x.toString) }).as(Right(Greeting("Done")))
    case "heinz" => IO(Left(KicksServiceGen.HelloError.appError(AppError())))
    case t       => IO(Right(Greeting(s"Hello $name from $t!")))
  }
}

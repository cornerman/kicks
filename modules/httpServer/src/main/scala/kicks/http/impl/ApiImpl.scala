package kicks.http.impl

import cats.effect.IO
import doobie.implicits.*
import doobie.util.transactor.Transactor
import kicks.api.{AppError, Greeting, KicksServiceGen}
import kicks.db.Db
import kicks.db.quill.schema.Foo

class ApiImpl(xa: Transactor[IO]) extends KicksServiceGen[[_, E, A, _, _] =>> IO[Either[E, A]]] {
  override def hello(name: String, town: Option[String]) = name match {
    case "db"    => Db.fun(Foo(Some("foos"))).transact(xa).as(Right(Greeting("Done")))
    case "heinz" => IO(Left(KicksServiceGen.HelloError.appError(AppError())))
    case t       => IO(Right(Greeting(s"Hello $name from $t!")))
  }
}

package kicks.http.impl

import cats.effect.IO
import kicks.api.{AppError, Greeting, KicksServiceGen}
import kicks.db.Db

import javax.sql.DataSource

class ApiImpl(dataSource: DataSource) extends KicksServiceGen[[_, E, A, _, _] =>> IO[Either[E, A]]] {
  override def hello(name: String, town: Option[String]) = name match {
    case "db"    => IO.blocking(Db.run(dataSource)).as(Right(Greeting("Done")))
    case "heinz" => IO(Left(KicksServiceGen.HelloError.appError(AppError())))
    case t       => IO(Right(Greeting(s"Hello $name from $t!")))
  }
}

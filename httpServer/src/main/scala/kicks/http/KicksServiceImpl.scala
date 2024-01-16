package kicks.http

import cats.effect.IO
import smithy4s.kicks.{AppError, Greeting, KicksServiceGen}

object KicksServiceImpl extends KicksServiceGen[ServiceTypes.SingleResponse] {
  override def hello(name: String, town: Option[String]) = IO.pure {
    town match {
      case Some("heinz") => Left(KicksServiceGen.HelloError.appError(AppError()))
      case Some(t)       => Right(Greeting(s"Hello $name from $t!"))
      case None          => Right(Greeting(s"Hello $name!"))
    }
  }
}

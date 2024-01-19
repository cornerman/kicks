package kicks.http

import scala.util.control.NoStackTrace

sealed abstract class AppError(msg: String, cause: Option[Throwable] = None) extends NoStackTrace { self: Product =>
  override def getCause: Throwable = cause.orNull
  override def getMessage: String  = msg
  override def toString: String    = s"AppError[${self.productPrefix}]: $msg ${cause.map(err => s"(cause: $err)").mkString}"
}
object AppError {
  case class Unexpected(error: Throwable) extends AppError("Unexpected error", cause = Some(error))
  case class InvalidApi(msg: String)      extends AppError(msg)
}

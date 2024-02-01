package kicks.http

import scala.util.control.NoStackTrace

sealed abstract class AppError(msg: String) extends NoStackTrace { self: Product =>
  override def getMessage: String = msg
  override def toString: String   = s"AppError[${self.productPrefix}]: $msg"
}
object AppError {
  case class InvalidApi(msg: String) extends AppError(msg)
}

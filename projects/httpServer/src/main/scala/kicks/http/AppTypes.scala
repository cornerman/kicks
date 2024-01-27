package kicks.http

import cats.effect.IO
import smithy4s.Transformation

object AppTypes {
  type ServiceResultBi[+ErrorOutput, +Output]        = IO[Either[ErrorOutput, Output]]
  type ServiceResult[_, +ErrorOutput, +Output, _, _] = ServiceResultBi[ErrorOutput, Output]

  val serviceResultTransform: Transformation.AbsorbError[ServiceResultBi, IO] = new Transformation.AbsorbError[ServiceResultBi, IO] {
    override def apply[E, A](fa: ServiceResultBi[E, A], injectError: E => Throwable): IO[A] = fa.flatMap {
      case Right(value) => IO.pure(value)
      case Left(error)  => IO.raiseError(injectError(error))
    }
  }
}

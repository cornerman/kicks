package kicks.http

import cats.effect.IO
import fs2.Stream
import smithy4s.Transformation

object AppTypes {
  type SingleResponseBi[+ErrorOutput, +Output] =
    IO[Either[ErrorOutput, Output]]
  type SingleResponse[_, +ErrorOutput, +Output, _, _] =
    SingleResponseBi[ErrorOutput, Output]
  type StreamResponseBi[+ErrorOutput, +StreamOutput] =
    IO[Either[ErrorOutput, Stream[IO, StreamOutput]]]
  type StreamResponse[_, ErrorOutput, _, _, StreamOutput] =
    StreamResponseBi[ErrorOutput, StreamOutput]

  val singleTransform = new Transformation.AbsorbError[AppTypes.SingleResponseBi, IO] {
    override def apply[E, A](fa: SingleResponseBi[E, A], injectError: E => Throwable): IO[A] = fa.flatMap {
      case Right(value) => IO.pure(value)
      case Left(error)  => IO.raiseError(injectError(error))
    }
  }

  val streamTransform = new Transformation.AbsorbError[AppTypes.StreamResponseBi, Stream[IO, *]] {
    override def apply[E, A](fa: StreamResponseBi[E, A], injectError: E => Throwable): Stream[IO, A] = Stream
      .eval(fa.map {
        case Right(value) => value
        case Left(error)  => Stream.raiseError[IO](injectError(error))
      })
      .flatten
  }
}

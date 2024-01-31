package kicks.http.http4sJsoniter

import cats.effect.{Async, Concurrent, Sync}
import cats.implicits.*
import com.github.plokhotnyuk.jsoniter_scala.core.{readFromArray, readFromStream, writeToArray, writeToStream, JsonValueCodec}
import fs2.io
import kicks.http.http4sJsoniter.CodeUtil.decodeResultFailure
import org.http4s.headers.`Content-Type`
import org.http4s.{DecodeFailure, DecodeResult, Entity, EntityDecoder, EntityEncoder, Headers, MalformedMessageBodyFailure, MediaType}
import zio.System.os

object ArrayEntityCodec {

  implicit def entityDecoder[F[_]: Concurrent, A](implicit codec: JsonValueCodec[A]): EntityDecoder[F, A] =
    EntityDecoder.decodeBy(MediaType.application.json) {
      EntityDecoder
        .byteArrayDecoder[F]
        .flatMapR { bytes =>
          DecodeResult(
            Concurrent[F].pure(
              Either.catchNonFatal(readFromArray(bytes)).leftMap(decodeResultFailure)
            )
          )
        }
        .decode(_, strict = true)
    }

  implicit def entityEncoder[F[_], A](implicit codec: JsonValueCodec[A]): EntityEncoder[F, A] =
    EntityEncoder.encodeBy[F, A](Headers(`Content-Type`(MediaType.application.json))) {
      EntityEncoder.byteArrayEncoder[F].contramap[A](writeToArray(_)).toEntity(_)
    }
}

package kicks.http.http4sJsoniter

import cats.effect.Async
import cats.implicits.*
import com.github.plokhotnyuk.jsoniter_scala.core.{readFromStream, writeToStream, JsonValueCodec}
import fs2.io
import org.http4s.headers.`Content-Type`
import org.http4s.{DecodeResult, Entity, EntityDecoder, EntityEncoder, Headers, MalformedMessageBodyFailure, MediaType}

object InputStreamEntityCodec {

  implicit def entityDecoder[F[_]: Async, A](implicit codec: JsonValueCodec[A]): EntityDecoder[F, A] =
    EntityDecoder.decodeBy[F, A](MediaType.application.json) { msg =>
      DecodeResult(io.toInputStreamResource(msg.body).use { is =>
        Async[F]
          .delay(readFromStream[A](is))
          .redeem(
            error => Left(MalformedMessageBodyFailure("Invalid JSON", Some(error))),
            value => Right(value),
          )
      })
    }

  implicit def entityEncoder[F[_]: Async, A](implicit codec: JsonValueCodec[A]): EntityEncoder[F, A] =
    EntityEncoder.encodeBy[F, A](Headers(`Content-Type`(MediaType.application.json))) { msg =>
      Entity(io.readOutputStream(1024) { os =>
        Async[F].delay(writeToStream(msg, os))
      })
    }
}

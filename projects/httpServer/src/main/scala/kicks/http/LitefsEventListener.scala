package kicks.http

import cats.effect.IO
import fs2.{Stream, text}
import com.github.plokhotnyuk.jsoniter_scala.core.JsonCodec
import org.http4s.client.Client
import org.http4s.implicits.http4sLiteralsSyntax
import org.http4s.{EventStream, Method, Request, ServerSentEvent, Uri}

import scala.concurrent.duration.DurationInt

object LitefsEventListener {
  def listen(client: Client[IO], host: String = "127.0.0.1", port: Int = 20202): Stream[IO, ServerSentEvent] = {
    val request = Request[IO](Method.GET, Uri.unsafeFromString(s"http://$host:$port/events"))
    Stream.eval(client.expect[EventStream[IO]](request)).flatten
  }
}

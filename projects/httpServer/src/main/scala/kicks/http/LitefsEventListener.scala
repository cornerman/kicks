package kicks.http

import cats.effect.IO
import fs2.Stream
import org.http4s.client.Client
import org.http4s.{Method, Request, ServerSentEvent, Uri}

object LitefsEventListener {
  def listen(client: Client[IO], host: String = "127.0.0.1", port: Int = 20202): Stream[IO, ServerSentEvent] = {
    val request = Request[IO](Method.GET, Uri.unsafeFromString(s"http://$host:$port/events"))
    client.stream(request).flatMap(_.body.through(ServerSentEvent.decoder[IO]))
  }
}

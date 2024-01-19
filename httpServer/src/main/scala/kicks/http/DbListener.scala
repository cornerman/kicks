package kicks.http

import cats.effect.IO
import fs2.Stream

import scala.concurrent.duration.DurationInt

class DbListener(state: AppState) {
  def subscribe(name: String): Stream[IO, AppEvent] = {
    Stream
      .emits(Seq(AppEvent.Hello("no")))
      .append(Stream.eval(IO.sleep(2.seconds) *> IO(AppEvent.Hello(name))))
  }
}

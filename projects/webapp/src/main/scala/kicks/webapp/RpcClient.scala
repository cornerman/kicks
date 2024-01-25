package kicks.webapp

import cats.effect.IO
import chameleon.{Deserializer, Serializer}
import colibri.{Cancelable, Observable}
import kicks.rpc.{EventRpc, RequestRpc}
import org.scalajs.dom
import sloth.{Client, Request, RequestTransport}

import scala.scalajs.js.URIUtils

object RpcClient {
  implicit val serializer: Serializer[String, String] = x => x
  implicit val deserializer: Deserializer[String, String] = x => Right(x)
  val requestRpc = Client[String, IO](RequestRpcTransport).wire[RequestRpc[IO]]
  val eventRpc = Client[String, Observable](EventRpcTransport).wire[EventRpc[Observable]]
}

private object RequestRpcTransport extends RequestTransport[String, IO] {
  override def apply(request: Request[String]): IO[String] = {
    IO.fromThenable(IO(
      dom.fetch(
        s"http://localhost:8080/${request.path.apiName}/${request.path.methodName}",
        new dom.RequestInit {
          method = dom.HttpMethod.POST //TODO GET? But how to encode payload? get needed for litefs routing to replica
          body = request.payload
        },
      ).`then`[String](_.text())
    ))
  }
}

private object EventRpcTransport extends RequestTransport[String, Observable] {
  override def apply(request: Request[String]): Observable[String] = Observable.create[String] { observer =>
    val pathPart = URIUtils.encodeURIComponent(request.payload)
    val source = new dom.EventSource(s"http://localhost:8080/${request.path.apiName}/${request.path.methodName}?payload=${pathPart}")

    source.onerror = { ev =>
      observer.unsafeOnError(new Exception(s"Failed EventSource (${ev.filename}:${ev.lineno}:${ev.colno}): ${ev.message}"))
    }
    source.onmessage = { ev =>
      observer.unsafeOnNext(ev.data.toString)
    }

    Cancelable(source.close)
  }
}

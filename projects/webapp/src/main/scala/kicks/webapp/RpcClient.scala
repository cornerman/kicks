package kicks.webapp

import cats.effect.IO
import chameleon.{Deserializer, Serializer}
import colibri.jsdom.EventSourceObservable
import colibri.{Cancelable, Observable}
import kicks.rpc.{EventRpc, Rpc}
import org.scalajs.dom
import sloth.{Client, Request, RequestTransport}

import scala.scalajs.js.URIUtils

object RpcClient {
  implicit val serializer: Serializer[String, String]     = x => x
  implicit val deserializer: Deserializer[String, String] = x => Right(x)
  val requestRpc                                          = Client[String, IO](RequestRpcTransport).wire[Rpc[IO]]
  val eventRpc                                            = Client[String, Observable](EventRpcTransport).wire[EventRpc[Observable]]
}

private object RequestRpcTransport extends RequestTransport[String, IO] {
  override def apply(request: Request[String]): IO[String] = {
    IO.fromThenable(
      IO(
        dom
          .fetch(
            s"http://localhost:8080/${request.path.apiName}/${request.path.methodName}",
            new dom.RequestInit {
              method = dom.HttpMethod.POST // TODO GET? But how to encode payload? get needed for litefs routing to replica
              body = request.payload
            },
          )
          .`then`[String](_.text())
      )
    )
  }
}

private object EventRpcTransport extends RequestTransport[String, Observable] {
  override def apply(request: Request[String]): Observable[String] = {
    val pathPart = URIUtils.encodeURIComponent(request.payload)
    val url      = s"http://localhost:8080/${request.path.apiName}/${request.path.methodName}?payload=${pathPart}"
    EventSourceObservable(url).map(_.data.toString)
  }
}

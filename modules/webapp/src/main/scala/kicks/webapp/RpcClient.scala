package kicks.webapp

import cats.effect.IO
import colibri.jsdom.EventSourceObservable
import colibri.Observable
import kicks.rpc.{EventRpc, Rpc}
import org.scalajs.dom
import sloth.ext.http4s.client.*
import colibri.ext.fs2.*
import org.http4s.dom.*

import scala.scalajs.js.URIUtils

object RpcClient {
  import kicks.shared.JsonPickler.*

  private val httpConfig    = IO.pure(HttpRequestConfig())
  private val fetchClient   = FetchClientBuilder[IO].create
  private val requestClient = sloth.Client[String, IO](HttpRpcTransport(fetchClient, httpConfig))
  private val eventClient   = sloth.Client[String, Observable](HttpRpcTransport.eventStream(fetchClient, httpConfig).map(Observable.lift))

  val requestRpc: Rpc[IO]            = requestClient.wire[Rpc[IO]]
  val eventRpc: EventRpc[Observable] = eventClient.wire[EventRpc[Observable]]
}

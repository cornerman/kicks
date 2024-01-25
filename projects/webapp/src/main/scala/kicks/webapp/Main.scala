package kicks.webapp

import cats.implicits._
import cats.effect.{IO, IOApp}
import outwatch.{Outwatch, VNode}
import kicks.webapp.state.AppState

object Main extends IOApp.Simple {
  LoadCss()

  private val app: VNode = {
    import outwatch.dsl._

    sloth.internal.RouterMacro

    val state = AppState()
    div(
      RpcClient.requestRpc.foo("wolf"),
      RpcClient.eventRpc.foo("peter"),
      App.layout.provide(state)
    )
  }

  override def run = {
    Outwatch.renderReplace[IO]("#app", app)
  }
}

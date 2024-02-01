package kicks.webapp

import cats.effect.{IO, IOApp}
import kicks.webapp.state.AppState
import outwatch.{Outwatch, VNode}

object Main extends IOApp.Simple {

  private val app: VNode = {
    import outwatch.dsl.*

    sloth.internal.RouterMacro

    val state = AppState()
    div(
      RpcClient.requestRpc.foo("wolf"),
      RpcClient.eventRpc.foo("peter"),
      App.layout.provide(state),
    )
  }

  override def run = {
    Outwatch.renderReplace[IO]("#app", app)
  }
}

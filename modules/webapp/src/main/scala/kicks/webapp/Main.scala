package kicks.webapp

import authn.frontend.*
import authn.frontend.authnJS.keratinAuthn.distTypesMod.Credentials
import cats.effect.{IO, IOApp}
import colibri.Observable
import kicks.shared.{AppConfig, JsonPickler}
import org.scalajs.dom
import outwatch.{Outwatch, VNode}

import scala.scalajs.js

object Main extends IOApp.Simple {

  private def app(state: AppState): VNode = {
    import colibri.*
    import outwatch.*
    import outwatch.dsl.*

    div(
      RpcClient.requestRpc.getPostThread(0).map(_.toString),
      RpcClient.eventRpc.foo("peter"),
      App.layout.provide(state),
      button(
        "Register",
        onClick.doEffect {
          state.authn.signup(Credentials(username = "est", password = "wolfgang254!!??"))
        },
      ),
      button(
        "Login",
        onClick.doEffect {
          state.authn.login(Credentials(username = "est", password = "wolfgang254!!??"))
        },
      ),
      b(state.authn.session),
      button(
        "Logout",
        onClick.doEffect {
          state.authn.logout
        },
      ),
    )
  }

  override def run: IO[Unit] = lift {
    val configJs = dom.window.asInstanceOf[js.Dictionary[js.Any]](AppConfig.domWindowProperty)
    val config   = JsonPickler.read[AppConfig](js.JSON.stringify(configJs))

    val state = AppState(config)

    !state.authn.restoreSession.voidError
    !Outwatch.renderReplace[IO]("#app", app(state))
  }
}

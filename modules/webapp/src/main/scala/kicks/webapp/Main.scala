package kicks.webapp

import authn.frontend.*
import authn.frontend.authnJS.keratinAuthn.distTypesMod.Credentials
import cats.effect.{IO, IOApp}
import cps.*
import cps.monads.catsEffect.given
import cps.syntax.unary_!
import kicks.shared.AppConfig
import org.scalajs.dom
import com.github.plokhotnyuk.jsoniter_scala.core.readFromString
import outwatch.{Outwatch, VNode}

import scala.scalajs.js

object Main extends IOApp.Simple {

  private def app(state: AppState): VNode = {
    import outwatch.dsl.*

    div(
      RpcClient.requestRpc.foo("wolf"),
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

  override def run = async[IO] {
    val configJs = dom.window.asInstanceOf[js.Dictionary[js.Any]](AppConfig.domWindowProperty)
    val config   = readFromString[AppConfig](js.JSON.stringify(configJs))

    val state = AppState(config)

    !state.authn.restoreSession.voidError
    !Outwatch.renderReplace[IO]("#app", app(state))
  }
}

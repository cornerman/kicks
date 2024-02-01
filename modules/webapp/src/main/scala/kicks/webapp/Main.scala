package kicks.webapp

import cps.*
import cps.monads.catsEffect.given
import cps.syntax.unary_!
import cats.effect.{Async, IO, IOApp}
import kicks.webapp.state.AppState
import outwatch.{Outwatch, VNode}
import authn.frontend.*
import authn.frontend.authnJS.keratinAuthn.distTypesMod.Credentials

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
      button(
        "Logout",
        onClick.doEffect {
          state.authn.logout
        },
      ),
    )
  }

  override def run = async[IO] {
    val state = AppState()

    !state.authn.restoreSession.voidError
    !Outwatch.renderReplace[IO]("#app", app(state))
  }
}

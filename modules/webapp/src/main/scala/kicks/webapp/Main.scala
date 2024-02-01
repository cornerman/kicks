package kicks.webapp

import cps.*
import cps.monads.catsEffect.given
import cps.syntax.unary_!
import cats.effect.{Async, IO, IOApp}
import kicks.webapp.state.AppState
import outwatch.{Outwatch, VNode}
import typings.keratinAuthn.mod as authn
import typings.keratinAuthn.distTypesMod.*

object Main extends IOApp.Simple {

  private val app: VNode = {
    import outwatch.dsl.*

    val state = AppState()
    div(
      RpcClient.requestRpc.foo("wolf"),
      RpcClient.eventRpc.foo("peter"),
      App.layout.provide(state),
      button(
        "Register",
        onClick.doAction {
          authn.signup(Credentials(username = "est", password = "wolfgang254!!??"))
        },
      ),
      button(
        "Login",
        onClick.doAction {
          authn.login(Credentials(username = "est", password = "wolfgang254!!??"))
        },
      ),
      button(
        "Logout",
        onClick.doAction {
          authn.logout()
        },
      ),
    )
  }

  private val setupAuthn: IO[Unit] = IO
    .fromThenable(IO {
      authn.setHost("http://localhost:3000")
      authn.setLocalStorageStore("kickssession")

      authn.restoreSession()
    })
    .attempt
    .map(println(_))

  override def run = async[IO] {
    !setupAuthn
    !Outwatch.renderReplace[IO]("#app", app)
  }
}

package kicks.webapp

import cats.implicits._
import cats.effect.{IO, IOApp}
import outwatch.{Outwatch, VNode}
import funstack.client.web.Fun
import kicks.webapp.state.{AppState, Auth}

object Main extends IOApp.Simple {
  LoadCss()

  private val app: VNode = {
    import outwatch.dsl._

    div(
      Fun.auth.currentUser.map { user =>
        val state = AppState(Auth(user))
        App.layout.provide(state)
      }
    )
  }

  override def run = {
    Fun.wsRpc.start &> Outwatch.renderReplace[IO]("#app", app)
  }
}

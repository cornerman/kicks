package kicks.webapp.state

import authn.frontend.{AuthnClient, AuthnClientConfig, SessionStorage}
import cats.effect.IO
import colibri.reactive.{Rx, Var}
import outwatch.EventDispatcher

sealed trait AppCommand
object AppCommand {
  case class AddTodo(title: String) extends AppCommand
  case object DeleteAllTodos        extends AppCommand
}

case class SubState()

case class AppState(
//  subcomponent2: SubState,
) extends EventDispatcher.Callback[AppCommand] {
  
  private object vars {
    val todos        = Var(Vector.empty[String])
    val subcomponent = Var(SubState())
  }

  def todos: Rx[Vector[String]]  = vars.todos
  def subcomponent: Rx[SubState] = vars.subcomponent

  override def dispatchOne(source: AppCommand): Unit = source match {
    case AppCommand.AddTodo(todo)  => vars.todos.update(_ ++ Vector(todo))
    case AppCommand.DeleteAllTodos => vars.todos.set(Vector.empty)
  }

  val authn = AuthnClient[IO](
    AuthnClientConfig(
      hostUrl = "http://localhost:3000",
      sessionStorage = SessionStorage.LocalStorage("kickssession"),
    )
  )
}

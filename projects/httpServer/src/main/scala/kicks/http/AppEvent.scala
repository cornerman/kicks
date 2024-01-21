package kicks.http

sealed trait AppEvent

object AppEvent {
  case class Hello(name: String) extends AppEvent
}

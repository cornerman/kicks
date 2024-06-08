package kicks.http

import scribe.*

object Logging {
  val minimumLevel = Option(System.getenv("LOG_LEVEL"))

  def setup(): Unit = {
    val _ = Logger.root.withMinimumLevel(minimumLevel.fold(Level.Info)(Level.apply)).replace()
  }
}

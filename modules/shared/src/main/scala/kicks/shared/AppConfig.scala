package kicks.shared

import kicks.shared.JsonPickler.ReadWriter

// this config class is shared between the backend and the frontend.
// the frontend requests it on page load from the backend in the index.html.
case class AppConfig(
  authnUrl: String
) derives ReadWriter

object AppConfig {
  def domWindowProperty: String = "AppConfig"
}

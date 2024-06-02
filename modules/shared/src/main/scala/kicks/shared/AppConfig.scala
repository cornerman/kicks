package kicks.shared

import com.github.plokhotnyuk.jsoniter_scala.core.JsonValueCodec
import com.github.plokhotnyuk.jsoniter_scala.macros.JsonCodecMaker

// this config class is shared between the backend and the frontend.
// the frontend requests it on page load from the backend in the index.html.
case class AppConfig(
  authnUrl: String
)
object AppConfig {
  given JsonValueCodec[AppConfig] = JsonCodecMaker.make

  def domWindowProperty: String = "AppConfig"
}

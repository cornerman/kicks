package kicks.shared

import com.github.plokhotnyuk.jsoniter_scala.core.JsonValueCodec
import com.github.plokhotnyuk.jsoniter_scala.macros.JsonCodecMaker

case class AppConfig(
  authnUrl: String
)
object AppConfig {
  implicit val codec: JsonValueCodec[AppConfig] = JsonCodecMaker.make

  def domWindowProperty: String = "AppConfig"
}

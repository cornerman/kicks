package kicks.webapp

import com.github.plokhotnyuk.jsoniter_scala.core.readFromString
import kicks.shared.AppConfig
import org.scalajs.dom

import scala.scalajs.js

object AppConfigLoader {
  def fromDomOrThrow(): AppConfig = {
    val appConfig     = dom.window.asInstanceOf[js.Dictionary[js.Object]](AppConfig.domWindowProperty)
    val appConfigJson = js.JSON.stringify(appConfig)
    readFromString(appConfigJson)
  }
}

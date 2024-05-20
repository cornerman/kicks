package kicks.webapp

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object Test {

  @JSImport("@shoelace-style/shoelace/dist/components/checkbox/checkbox.js", JSImport.Namespace)
  @js.native
  object RawImport extends js.Object

  import outwatch.dsl.*
  import outwatch.*
  def foo() = {
    RawImport
    VNode.html("sl-checkbox")(
      "harals",
      EmitterBuilder
        .fromEvent[org.scalajs.dom.Event]("sl-change")
        .foreach(e => org.scalajs.dom.console.log("hallo", e.target.asInstanceOf[js.Dynamic].checked)),
    )

//    Checkbox.tag(
//      Checkbox.onSlChange.value.foreach(dom.console.log(_)),
//
//      slIcon(slot := "prefix")
//    )

  }

}

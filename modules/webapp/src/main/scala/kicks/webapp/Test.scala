package kicks.webapp

import outwatch.dsl.*
import outwatch.*
import org.scalajs.dom

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object Test {

  def foo() = {
    import kicks.web.emojipicker.Picker.*
    picker(
      onEmojiClick.foreach(e => dom.console.log("hallo", e.detail))
    )
  }

  def bar() = {
    import kicks.web.shoelace.SlCheckbox.*
    import kicks.web.shoelace.SlColorPicker
    import kicks.web.shoelace.SlTree
    import kicks.web.shoelace.SlTreeItem
    import kicks.web.shoelace.SlButton

    VMod(
      SlButton.slButton(
        "Harl",
        onClick.doAction(println("HI")),
      ),
      slCheckbox(
        "harals 2",
        div(slotHelpText, "Help text"),
        onSlChange.foreach(e => dom.console.log("hallo", e.target.checked)),
      ),
      
      SlColorPicker.slColorPicker(SlColorPicker.sliderHeight := "10"),
    )

  }

}

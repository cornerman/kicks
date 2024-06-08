package kicks.webapp

import org.scalajs.dom
import outwatch.*
import outwatch.dsl.*

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
    import kicks.web.shoelace.{SlButton, SlColorPicker, SlTree, SlTreeItem}

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

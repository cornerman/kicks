package kicks.webapp

import colibri.Observable
import kicks.webapp.state.{AppCommand, AppState, SubState}
import outwatch.*
import outwatch.dsl.*

object App {

  // For styling, we use tailwindcss and daisyui:
  // - https://tailwindcss.com/ - basic styles like p-5, space-x-2, mb-auto, ...
  // - https://daisyui.com/ - based on tailwindcss with components like btn, navbar, footer, ...

  def mySub: VModM[SubState] = VMod(
    VMod.access[SubState](s => s.toString)
  )

  def layout: VNodeM[AppState] =
    div(
      "foo",
      mySub.provideSomeF[Observable, AppState](_.subcomponent.observable),
      cls := "flex flex-col h-screen",
      pageHeader,
      pageBody,
      pageFooter,
      VMod.access[AppState](state => state.todos),
      button(
        "Add a Todo",
        onClick.as(AppCommand.AddTodo("Do something")).dispatch,
      ),
      button(
        "Delete all Todos",
        onClick.as(AppCommand.DeleteAllTodos).dispatch,
      ),
    )

  def pageHeader: VNodeM[AppState] =
    header(
      div(
        pageLink("Home", Page.Home),
        pageLink("API", Page.Api)(cls := "nav-api"),
        cls := "space-x-2",
      ),
      div(
        authControls,
        cls := "ml-auto",
      ),
      cls := "navbar shadow-lg",
    )

  def authControls = div("auth")

  def pageLink(name: String, page: Page) = {
    val styling = Page.current.map {
      case `page` => cls := "btn-neutral"
      case _      => cls := "btn-ghost"
    }

    a(cls := "btn", name, page.href, styling)
  }

  def pageBody = div(
    cls := "p-10 mb-auto",
    Page.current.map {
      case Page.Home => div("HOME")
      case Page.Api  => div("AHJA")
    },
  )

  def pageFooter =
    footer(
      cls := "p-5 footer bg-base-200 text-base-content footer-center",
      div(
        cls := "flex flex-row space-x-4",
        a(cls := "link link-hover", href := "#", "About us"),
        a(cls := "link link-hover", href := "#", "Contact"),
      ),
    )
}

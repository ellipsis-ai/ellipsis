package services.ms_teams.apiModels

import play.api.libs.json.JsValue

sealed trait CardElement {

}

case class ActionSubmit(
                       title: String,
                       data: JsValue
                     ) extends CardElement {
  val `type`: String = "Action.Submit"
}

case class TextBlock(text: String) extends CardElement {
  val `type`: String = "TextBlock"
}

case class InputChoice(title: String, value: String) {
  val `type`: String = "Input.Choice"
}

case class InputChoiceSet(
                           id: String,
                           value: String,
                           choices: Seq[InputChoice]
                         ) extends CardElement {
  val `type`: String = "Input.ChoiceSet"
}

case class InputText(
                    id: String
                    ) extends CardElement {
  val `type`: String = "Input.Text"
}

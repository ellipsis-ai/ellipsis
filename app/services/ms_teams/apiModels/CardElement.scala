package services.ms_teams.apiModels

import play.api.libs.json.JsValue

sealed trait CardElement {

}

case class CardAction(
                       `type`: String,
                       title: String,
                       data: JsValue
                     ) extends CardElement

case class TextBlock(`type`: String, text: String) extends CardElement

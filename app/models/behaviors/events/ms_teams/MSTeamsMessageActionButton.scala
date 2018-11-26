package models.behaviors.events.ms_teams

import models.behaviors.events.MessageActionButton
import play.api.libs.json.{JsObject, Json}
import services.ms_teams.apiModels.{ActionSubmit, CardElement}

case class MSTeamsMessageActionButton(
                                       text: String,
                                       name: String,
                                       value: JsObject,
                                       maybeStyle: Option[String] = None
                                     ) extends MSTeamsMessageAction with MessageActionButton {

  def bodyElements: Seq[CardElement] = Seq()
  def actionElements: Seq[CardElement] = Seq(ActionSubmit(text, Json.obj("actionName" -> name) ++ value))
}

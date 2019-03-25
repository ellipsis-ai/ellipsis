package models.behaviors.events.ms_teams

import models.behaviors.events.Message

case class MSTeamsMessage(id: String) extends Message {
  val maybeId: Option[String] = Some(id)
  val maybeThreadId: Option[String] = None
}

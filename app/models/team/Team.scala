package models.team

import play.api.libs.json.Json

case class Team(
                 id: String,
                 name: String
               ) {

  def maybeNonEmptyName: Option[String] = Option(name).filter(_.trim.nonEmpty)
}

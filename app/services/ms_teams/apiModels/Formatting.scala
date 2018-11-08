package services.ms_teams.apiModels

import play.api.libs.json._

object Formatting {

  lazy implicit val slackCommentFormat: Format[MSTeamsOrganization] = Json.format[MSTeamsOrganization]

}

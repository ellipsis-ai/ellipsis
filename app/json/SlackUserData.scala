package json

import play.api.libs.json.JsObject

case class SlackUserData(accountId: String, accountTeamId: String, accountName: String, tz: Option[String], profile: JsObject)

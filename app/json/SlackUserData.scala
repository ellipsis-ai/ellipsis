package json

import play.api.libs.json.JsObject

case class SlackUserData(
                          accountId: String,
                          accountTeamId: String,
                          accountName: String,
                          fullName: Option[String],
                          tz: Option[String],
                          deleted: Boolean,
                          profile: JsObject
                        )

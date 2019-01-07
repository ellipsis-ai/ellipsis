package models.behaviors.ellipsisobject

import models.behaviors.ellipsisobject.EllipsisObjectConstants.{ENV_KEY, EVENT_KEY, TOKEN_KEY}
import models.behaviors.invocationtoken.InvocationToken
import models.environmentvariable.{EnvironmentVariable, TeamEnvironmentVariable}
import play.api.libs.json._
import services.AWSLambdaConstants._

case class EllipsisObject(
                           userInfo: DeprecatedUserInfo,
                           teamInfo: TeamInfo,
                           eventInfo: EventInfo,
                           environmentVariables: Seq[EnvironmentVariable],
                           apiBaseUrl: String,
                           token: InvocationToken
                         ) {

  def toJson: JsObject = {
    val teamEnvVars = environmentVariables.filter(ev => ev.isInstanceOf[TeamEnvironmentVariable])
    Json.obj(
      API_BASE_URL_KEY -> JsString(apiBaseUrl),
      TOKEN_KEY -> JsString(token.id),
      ENV_KEY -> JsObject(teamEnvVars.map { ea =>
        ea.name -> JsString(ea.value)
      }),
      USER_INFO_KEY -> userInfo.toJson,
      TEAM_INFO_KEY -> teamInfo.toJson, // deprecated
      TEAM_KEY -> teamInfo.toJson,
      EVENT_KEY -> eventInfo.toJson
    )
  }
}

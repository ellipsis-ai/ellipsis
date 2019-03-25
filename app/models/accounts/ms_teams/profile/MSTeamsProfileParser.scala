package models.accounts.ms_teams.profile

import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.impl.providers.{OAuth2Info, SocialProfileParser}
import play.api.libs.json._

import scala.concurrent.Future

case class MSTeamsProfileParseException(json: JsValue, message: String) extends Exception(message)

class MSTeamsProfileParser extends SocialProfileParser[JsValue, MSTeamsProfile, OAuth2Info] {
  import models.accounts.ms_teams.MSTeamsProvider._

  def parse(json: JsValue, authInfo: OAuth2Info): Future[MSTeamsProfile] = Future.successful {
    val userId = (json \ "id").as[String]
    val teamId = (json \ "org" \ "value" \ 0 \ "id").as[String]
    val loginInfo = LoginInfo(ID, userId)
    MSTeamsProfile(
      teamId,
      loginInfo = loginInfo
    )
  }

}

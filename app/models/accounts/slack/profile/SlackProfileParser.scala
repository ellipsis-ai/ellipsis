package models.accounts.slack.profile

import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.impl.exceptions.ProfileRetrievalException
import com.mohiva.play.silhouette.impl.providers.{OAuth2Info, SocialProfileParser}
import models.accounts.slack.{SlackProvider, SlackUserTeamIds}
import play.api.Logger
import play.api.libs.json._

import scala.concurrent.Future

case class SlackProfileParseException(json: JsValue, message: String) extends Exception(message)

object ParseType extends Enumeration {
  val ParseForInstall, ParseForSignIn = Value
}

class SlackProfileParser extends SocialProfileParser[JsValue, SlackProfile, OAuth2Info] {
  import SlackProvider._

  def parse(json: JsValue, authInfo: OAuth2Info): Future[SlackProfile] = parseForSignIn(json, authInfo)

  def getProfileException(parseType: ParseType.Value, json: JsValue, authInfo: OAuth2Info): ProfileRetrievalException = {
    val maybeError = (json \ "error").asOpt[String]
    val message = maybeError.getOrElse("error")
    val exception = SlackProfileParseException(json, message)
    val params = SlackProvider.dumpParamsFromAuthInfo(authInfo)
    Logger.error(s"Error parsing Slack profile during auth attempt for ${parseType.toString}. ${params}", exception)
    new ProfileRetrievalException(message, exception)
  }

  def parseForInstall(json: JsValue, authInfo: OAuth2Info): Future[SlackProfile] = Future.successful {
    val success = (json \ "ok").as[Boolean]

    if (success) {
      val maybeEnterpriseId = (json \ "enterprise_id").asOpt[String]
      val teamId = (json \ "team_id").as[String]
      val userId = (json \ "user_id").as[String]
      val loginInfo = LoginInfo(ID, userId)
      SlackProfile(
        SlackUserTeamIds(teamId),
        loginInfo = loginInfo,
        maybeEnterpriseId
      )
    } else {
      throw getProfileException(ParseType.ParseForInstall, json, authInfo)
    }
  }

  def parseForSignIn(json: JsValue, authInfo: OAuth2Info): Future[SlackProfile] = Future.successful {
    val success = (json \ "ok").as[Boolean]

    if (success) {
      val maybeEnterpriseId = (json \ "enterprise_id").asOpt[String]
      val userId = (json \ "user" \ "id").as[String]
      val teamId = (json \ "team" \ "id").as[String]
      val loginInfo = LoginInfo(ID, userId)
      SlackProfile(
        SlackUserTeamIds(teamId),
        loginInfo = loginInfo,
        maybeEnterpriseId
      )
    } else {
      throw getProfileException(ParseType.ParseForSignIn, json, authInfo)
    }
  }

}

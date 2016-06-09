package models.accounts

import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.impl.exceptions.ProfileRetrievalException
import com.mohiva.play.silhouette.impl.providers.SocialProfileParser
import play.api.libs.json._

import scala.concurrent.Future

case class SlackProfileParseException(json: JsValue, message: String) extends Exception(message)

class SlackProfileParser extends SocialProfileParser[JsValue, SlackProfile] {
  import SlackProvider._

  def parse(json: JsValue): Future[SlackProfile] = parseForSignIn(json)

  def parseForInstall(json: JsValue): Future[SlackProfile] = Future.successful {
    val success = (json \ "ok").as[Boolean]

    if (success) {
      val teamUrl = (json \ "url").asOpt[String]
      val teamName = (json \ "team").asOpt[String]
      val userName = (json \ "user").asOpt[String]
      val teamId = (json \ "team_id").as[String]
      val userId = (json \ "user_id").as[String]
      val loginInfo = LoginInfo(ID, userId)
      SlackProfile(
        maybeTeamUrl = teamUrl,
        maybeTeamName = teamName,
        maybeUserName = userName,
        teamId = teamId,
        loginInfo = loginInfo)
    } else {
      val maybeError = (json \ "error").asOpt[String]
      val message = maybeError.getOrElse("error")
      throw new ProfileRetrievalException(message, SlackProfileParseException(json, message))
    }
  }

  def parseForSignIn(json: JsValue): Future[SlackProfile] = Future.successful {
    val success = (json \ "ok").as[Boolean]

    if (success) {
      val userId = (json \ "user" \ "id").as[String]
      val teamId = (json \ "team" \ "id").as[String]
      val loginInfo = LoginInfo(ID, userId)
      SlackProfile(
        maybeTeamUrl = None,
        maybeTeamName = None,
        maybeUserName = None,
        teamId = teamId,
        loginInfo = loginInfo)
    } else {
      val maybeError = (json \ "error").asOpt[String]
      val message = maybeError.getOrElse("error")
      throw new ProfileRetrievalException(message, SlackProfileParseException(json, message))
    }
  }

}

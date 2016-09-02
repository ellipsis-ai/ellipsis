package models.accounts

import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.impl.exceptions.ProfileRetrievalException
import com.mohiva.play.silhouette.impl.providers.{OAuth2Info, SocialProfileParser}
import play.api.libs.json._

import scala.concurrent.Future

case class SlackProfileParseException(json: JsValue, message: String) extends Exception(message)

class SlackProfileParser extends SocialProfileParser[JsValue, SlackProfile, OAuth2Info] {
  import SlackProvider._

  def parse(json: JsValue, authInfo: OAuth2Info): Future[SlackProfile] = parseForSignIn(json)

  def parseForInstall(json: JsValue): Future[SlackProfile] = Future.successful {
    val success = (json \ "ok").as[Boolean]

    if (success) {
      val teamId = (json \ "team_id").as[String]
      val userId = (json \ "user_id").as[String]
      val loginInfo = LoginInfo(ID, userId)
      SlackProfile(
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
        teamId = teamId,
        loginInfo = loginInfo)
    } else {
      val maybeError = (json \ "error").asOpt[String]
      val message = maybeError.getOrElse("error")
      throw new ProfileRetrievalException(message, SlackProfileParseException(json, message))
    }
  }

}

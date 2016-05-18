package models.accounts

import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.impl.exceptions.ProfileRetrievalException
import com.mohiva.play.silhouette.impl.providers.SocialProfileParser
import play.api.libs.json._

import scala.concurrent.Future

case class SlackProfileParseException(json: JsValue, message: String) extends Exception(message)

class SlackProfileParser extends SocialProfileParser[JsValue, SlackProfile] {
  import SlackProvider._

  override def parse(json: JsValue): Future[SlackProfile] = Future.successful {
    val success = (json \ "ok").as[Boolean]

    if (success) {
      val teamUrl = (json \ "url").as[String]
      val teamName = (json \ "team").as[String]
      val userName = (json \ "user").as[String]
      val teamId = (json \ "team_id").as[String]
      val userId = (json \ "user_id").as[String]
      val loginInfo = LoginInfo(ID, userId)
      SlackProfile(
        teamUrl = teamUrl,
        teamName = teamName,
        userName = userName,
        teamId = teamId,
        loginInfo = loginInfo)
    } else {
      val maybeError = (json \ "error").asOpt[String]
      val message = maybeError.getOrElse("error")
      throw new ProfileRetrievalException(message, SlackProfileParseException(json, message))
    }
  }

  def parseLoginInfo(json: JsValue): Future[LoginInfo] = Future.successful {
    val success = (json \ "ok").as[Boolean]

    if (success) {
      val userId = (json \ "user" \ "id").as[String]
      LoginInfo(ID, userId)
    } else {
      val maybeError = (json \ "error").asOpt[String]
      val message = maybeError.getOrElse("error")
      throw new ProfileRetrievalException(message, SlackProfileParseException(json, message))
    }
  }
}

package models.accounts.github.profile

import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.impl.providers.{OAuth2Info, SocialProfileParser}
import play.api.libs.json._

import scala.concurrent.Future

case class GithubProfileParserException(json: JsValue, message: String) extends Exception(message)

class GithubProfileParser extends SocialProfileParser[JsValue, GithubProfile, OAuth2Info] {
  import models.accounts.github.GithubProvider._

  def parse(json: JsValue, authInfo: OAuth2Info): Future[GithubProfile] = Future.successful {
    val userId = (json \ "id").as[Long].toString
    val loginInfo = LoginInfo(ID, userId)
    GithubProfile(
      loginInfo = loginInfo,
      token = authInfo.accessToken
    )
  }

}

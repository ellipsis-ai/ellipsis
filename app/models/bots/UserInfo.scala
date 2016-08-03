package models.bots

import com.mohiva.play.silhouette.api.LoginInfo
import models.accounts.{LinkedAccount, User, OAuth2Token}
import slick.dbio.DBIO
import play.api.libs.json._
import scala.concurrent.ExecutionContext.Implicits.global

case class LinkedInfo(loginInfo: LoginInfo, maybeOAuth2Token: Option[OAuth2Token]) {

  def toJson: JsObject = {
    var parts = Seq(
      "externalSystem" -> JsString(loginInfo.providerID),
      "userId" -> JsString(loginInfo.providerKey)
    )
    maybeOAuth2Token.foreach { oauth2Token =>
      parts = parts ++ Seq(
        "oauthToken" -> JsString(oauth2Token.accessToken)
      )
    }
    JsObject(parts)
  }

}

case class UserInfo(maybeUser: Option[User], links: Seq[LinkedInfo]) {

  def toJson: JsObject = {
    var parts: Seq[(String, JsValue)] = Seq(
      "links" -> JsArray(links.map(_.toJson))
    )
    maybeUser.foreach { user =>
      parts = parts ++ Seq("ellipsisUserId" -> JsString(user.id))
    }
    JsObject(parts)
  }
}

object UserInfo {

  def forLoginInfo(loginInfo: LoginInfo, teamId: String): DBIO[UserInfo] = {
    for {
      maybeLinkedAccount <- LinkedAccount.find(loginInfo, teamId)
      maybeUser <- DBIO.successful(maybeLinkedAccount.map(_.user))
      allLinkedAccounts <- maybeUser.map { user =>
        LinkedAccount.allFor(user)
      }.getOrElse(DBIO.successful(Seq()))
      links <- DBIO.sequence(allLinkedAccounts.map { linkedAccount =>
        val loginInfo = linkedAccount.loginInfo
        OAuth2Token.findByLoginInfo(loginInfo).map { maybeOAuth2Token =>
          LinkedInfo(loginInfo, maybeOAuth2Token)
        }
      })
    } yield {
      UserInfo(maybeUser, links)
    }
  }
}

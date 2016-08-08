package models.bots

import com.mohiva.play.silhouette.api.LoginInfo
import models.accounts._
import slick.dbio.DBIO
import play.api.libs.json._
import scala.concurrent.ExecutionContext.Implicits.global

case class LinkedInfo(externalSystem: String, accessToken: String) {

  def toJson: JsObject = {
    JsObject(Seq(
      "externalSystem" -> JsString(externalSystem),
      "oauthToken" -> JsString(accessToken)
    ))
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
      linkedTokens <- maybeUser.map { user =>
        LinkedOAuth2TokenQueries.allForUser(user)
      }.getOrElse(DBIO.successful(Seq()))
      links <- DBIO.successful(linkedTokens.map { ea =>
        LinkedInfo(ea.config.name, ea.accessToken)
      })
    } yield {
      UserInfo(maybeUser, links)
    }
  }
}

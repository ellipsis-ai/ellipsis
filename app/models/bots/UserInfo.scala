package models.bots

import com.mohiva.play.silhouette.api.LoginInfo
import models.accounts._
import models.accounts.user.User
import play.api.libs.ws.WSClient
import slick.dbio.DBIO
import play.api.libs.json._
import services.DataService

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

  def forLoginInfo(loginInfo: LoginInfo, teamId: String, ws: WSClient, dataService: DataService): DBIO[UserInfo] = {
    for {
      maybeLinkedAccount <- DBIO.from(dataService.linkedAccounts.find(loginInfo, teamId))
      maybeUser <- DBIO.successful(maybeLinkedAccount.map(_.user))
      linkedTokens <- maybeUser.map { user =>
        LinkedOAuth2TokenQueries.allForUser(user, ws)
      }.getOrElse(DBIO.successful(Seq()))
      links <- DBIO.successful(linkedTokens.map { ea =>
        LinkedInfo(ea.application.name, ea.accessToken)
      })
    } yield {
      UserInfo(maybeUser, links)
    }
  }
}

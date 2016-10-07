package models.behaviors

import com.mohiva.play.silhouette.api.LoginInfo
import models.accounts.oauth2application.OAuth2Application
import models.accounts.user.User
import models.team.Team
import play.api.libs.ws.WSClient
import play.api.libs.json._
import services.DataService

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

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

  def forLoginInfo(loginInfo: LoginInfo, teamId: String, ws: WSClient, dataService: DataService): Future[UserInfo] = {
    for {
      maybeLinkedAccount <- dataService.linkedAccounts.find(loginInfo, teamId)
      maybeUser <- Future.successful(maybeLinkedAccount.map(_.user))
      linkedTokens <- maybeUser.map { user =>
        dataService.linkedOAuth2Tokens.allForUser(user, ws)
      }.getOrElse(Future.successful(Seq()))
      links <- Future.successful(linkedTokens.map { ea =>
        LinkedInfo(ea.application.name, ea.accessToken)
      })
    } yield {
      UserInfo(maybeUser, links)
    }
  }

}

case class TeamInfo(team: Team, links: Seq[LinkedInfo]) {

  def toJson: JsObject = {
    val parts: Seq[(String, JsValue)] = Seq(
      "links" -> JsArray(links.map(_.toJson))
    )
    JsObject(parts)
  }

}

object TeamInfo {

  def forOAuth2Apps(apps: Seq[OAuth2Application], team: Team, ws: WSClient): Future[TeamInfo] = {
    Future.sequence(apps.map { ea =>
      ea.getClientCredentialsTokenFor(ws).map { maybeToken =>
        maybeToken.map { token =>
          LinkedInfo(ea.name, token)
        }
      }
    }).map { linkMaybes =>
      TeamInfo(team, linkMaybes.flatten)
    }
  }

}

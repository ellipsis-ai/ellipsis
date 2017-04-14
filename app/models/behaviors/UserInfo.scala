package models.behaviors

import akka.actor.ActorSystem
import models.accounts.oauth2application.OAuth2Application
import models.accounts.user.User
import models.behaviors.events.Event
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
      "token" -> JsString(accessToken),
      "oauthToken" -> JsString(accessToken)
    ))
  }

}

case class MessageInfo(medium: String, channel: Option[String], userId: String, details: JsObject)

object MessageInfo {

  def buildFor(event: Event, ws: WSClient, dataService: DataService)(implicit actorSystem: ActorSystem): Future[MessageInfo] = {
    event.detailsFor(ws, dataService).map { details =>
      MessageInfo(event.name, event.maybeChannel, event.userIdForContext, details)
    }
  }

}

case class UserInfo(
                     user: User,
                     links: Seq[LinkedInfo],
                     maybeMessageInfo: Option[MessageInfo]
                   ) {

  implicit val messageInfoWrites = Json.writes[MessageInfo]

  def toJson: JsObject = {
    val linkParts: Seq[(String, JsValue)] = Seq(
      "links" -> JsArray(links.map(_.toJson))
    )
    val messageInfoPart = maybeMessageInfo.map { info =>
      Seq("messageInfo" -> Json.toJson(info))
    }.getOrElse(Seq())
    val userParts = Seq("ellipsisUserId" -> JsString(user.id))
    JsObject(userParts ++ linkParts ++ messageInfoPart)
  }
}

object UserInfo {

  def buildFor(maybeUser: Option[User], event: Event, ws: WSClient, dataService: DataService)(implicit actorSystem: ActorSystem): Future[UserInfo] = {
    for {
      linkedOAuth2Tokens <- maybeUser.map { user =>
        dataService.linkedOAuth2Tokens.allForUser(user, ws)
      }.getOrElse(Future.successful(Seq()))
      linkedSimpleTokens <- maybeUser.map { user =>
        dataService.linkedSimpleTokens.allForUser(user)
      }.getOrElse(Future.successful(Seq()))
      links <- Future.successful {
        linkedOAuth2Tokens.map { ea =>
          LinkedInfo(ea.application.name, ea.accessToken)
        } ++ linkedSimpleTokens.map { ea =>
          LinkedInfo(ea.api.name, ea.accessToken)
        }
      }
      messageInfo <- event.messageInfo(ws, dataService)
      user <- maybeUser.map(Future.successful).getOrElse {
        event.ensureUser(dataService)
      }
    } yield {
      UserInfo(user, links, Some(messageInfo))
    }
  }

  def buildFor(event: Event, teamId: String, ws: WSClient, dataService: DataService)(implicit actorSystem: ActorSystem): Future[UserInfo] = {
    for {
      maybeLinkedAccount <- dataService.linkedAccounts.find(event.loginInfo, teamId)
      maybeUser <- Future.successful(maybeLinkedAccount.map(_.user))
      info <- buildFor(maybeUser, event, ws, dataService)
    } yield info
  }

}

case class TeamInfo(team: Team, links: Seq[LinkedInfo]) {

  def toJson: JsObject = {
    val linkParts: Seq[(String, JsValue)] = Seq(
      "links" -> JsArray(links.map(_.toJson))
    )
    val timeZonePart = Seq("timeZone" -> JsString(team.timeZone.toString))
    JsObject(linkParts ++ timeZonePart)
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

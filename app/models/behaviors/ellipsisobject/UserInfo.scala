package models.behaviors.ellipsisobject

import akka.actor.ActorSystem
import json.Formatting._
import json.UserData
import models.accounts.user.User
import models.behaviors.conversations.conversation.Conversation
import models.behaviors.events.{Event, MessageUserData}
import play.api.libs.json._
import services.DefaultServices
import slick.dbio.DBIO

import scala.concurrent.ExecutionContext

case class UserInfo(user: User, links: Seq[LinkedInfo], maybeMessageInfo: Option[MessageInfo], maybeUserData: Option[UserData]) {

  def toJson: JsObject = {
    val linkInfo = JsArray(links.map(_.toJson))
    val messageInfo = maybeMessageInfo.map(info => Json.toJsObject(info)).getOrElse(Json.obj())
    val userDataPart = Json.toJsObject(MessageUserData(
      maybeUserData.flatMap(_.context).getOrElse("unknown"),
      maybeUserData.map(_.userNameOrDefault).getOrElse("unknown"),
      Some(user.id),
      maybeUserData.flatMap(_.userIdForContext).orElse(maybeMessageInfo.map(_.userId)),
      maybeUserData.flatMap(_.fullName),
      maybeUserData.flatMap(_.email),
      maybeUserData.flatMap(_.tz)
    ))
    Json.obj(
      "links" -> linkInfo,
      "messageInfo" -> messageInfo
    ) ++ userDataPart
  }
}

object UserInfo {

  def buildForAction(
                      user: User,
                      event: Event,
                      maybeConversation: Option[Conversation],
                      services: DefaultServices
                    )(implicit actorSystem: ActorSystem, ec: ExecutionContext): DBIO[UserInfo] = {
    for {
      linkedOAuth1Tokens <- services.dataService.linkedOAuth1Tokens.allForUserAction(user, services.ws)
      linkedOAuth2Tokens <- services.dataService.linkedOAuth2Tokens.allForUserAction(user, services.ws)
      linkedSimpleTokens <- services.dataService.linkedSimpleTokens.allForUserAction(user)
      links <- DBIO.successful {
        linkedOAuth1Tokens.map { ea =>
          LinkedInfo(ea.application.name, ea.accessToken)
        } ++ linkedOAuth2Tokens.map { ea =>
          LinkedInfo(ea.application.name, ea.accessToken)
        } ++ linkedSimpleTokens.map { ea =>
          LinkedInfo(ea.api.name, ea.accessToken)
        }
      }
      messageInfo <- DBIO.from(event.messageInfo(maybeConversation, services))
      maybeTeam <- services.dataService.teams.findAction(user.teamId)
      maybeUserData <- maybeTeam.map { team =>
        DBIO.from(services.dataService.users.userDataFor(user, team)).map(Some(_))
      }.getOrElse(DBIO.successful(None))
    } yield {
      UserInfo(user, links, Some(messageInfo), maybeUserData)
    }
  }

  def buildForAction(
                      event: Event,
                      maybeConversation: Option[Conversation],
                      services: DefaultServices
                    )(implicit actorSystem: ActorSystem, ec: ExecutionContext): DBIO[UserInfo] = {
    for {
      user <- event.ensureUserAction(services.dataService)
      info <- buildForAction(user, event, maybeConversation, services)
    } yield info
  }

}

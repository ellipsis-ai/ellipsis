package models.behaviors.ellipsisobject

import akka.actor.ActorSystem
import json.Formatting._
import json.UserData
import models.accounts.user.User
import models.behaviors.conversations.conversation.Conversation
import models.behaviors.events.Event
import play.api.libs.json._
import services.DefaultServices
import slick.dbio.DBIO

import scala.concurrent.ExecutionContext

case class DeprecatedUserInfo(user: User, links: Seq[IdentityInfo], maybeMessageInfo: Option[DeprecatedMessageInfo], maybeUserData: Option[UserData]) {

  def toJson: JsObject = {
    val linkInfo = JsArray(links.map(_.toJson))
    val messageInfo = maybeMessageInfo.map(info => Json.toJsObject(info)).getOrElse(Json.obj())
    val userDataPart = maybeUserData.map(data => Json.toJsObject(data)).getOrElse(Json.obj())
    Json.obj(
      "links" -> linkInfo,
      "messageInfo" -> messageInfo
    ) ++ userDataPart
  }
}

object DeprecatedUserInfo {

  def buildForAction(
                      user: User,
                      event: Event,
                      maybeConversation: Option[Conversation],
                      services: DefaultServices
                    )(implicit actorSystem: ActorSystem, ec: ExecutionContext): DBIO[DeprecatedUserInfo] = {
    for {
      links <- IdentityInfo.allForAction(user, services)
      messageInfo <- event.deprecatedMessageInfoAction(maybeConversation, services)
      maybeTeam <- services.dataService.teams.findAction(user.teamId)
      maybeUserData <- maybeTeam.map { team =>
        DBIO.from(services.dataService.users.userDataFor(user, team)).map(Some(_))
      }.getOrElse(DBIO.successful(None))
    } yield {
      DeprecatedUserInfo(user, links, Some(messageInfo), maybeUserData)
    }
  }

  def buildForAction(
                      event: Event,
                      maybeConversation: Option[Conversation],
                      services: DefaultServices
                    )(implicit actorSystem: ActorSystem, ec: ExecutionContext): DBIO[DeprecatedUserInfo] = {
    for {
      user <- event.ensureUserAction(services.dataService)
      info <- buildForAction(user, event, maybeConversation, services)
    } yield info
  }

}

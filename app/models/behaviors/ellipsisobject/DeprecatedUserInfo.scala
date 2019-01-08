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

case class DeprecatedUserInfo(
                               links: Seq[IdentityInfo],
                               messageInfo: Option[DeprecatedMessageInfo],
                               ellipsisUserId: String,
                               context: Option[String],
                               userName: Option[String],
                               userIdForContext: Option[String],
                               fullName: Option[String],
                               email: Option[String],
                               timeZone: Option[String],
                               formattedLink: Option[String]
                              )

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
      DeprecatedUserInfo(
        links,
        Some(messageInfo),
        user.id,
        maybeUserData.flatMap(_.context),
        maybeUserData.flatMap(_.userName),
        maybeUserData.flatMap(_.userIdForContext),
        maybeUserData.flatMap(_.fullName),
        maybeUserData.flatMap(_.email),
        maybeUserData.flatMap(_.timeZone),
        maybeUserData.flatMap(_.formattedLink)
      )
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

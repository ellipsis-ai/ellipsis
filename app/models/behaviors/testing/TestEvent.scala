package models.behaviors.testing

import akka.actor.ActorSystem
import models.accounts.user.User
import models.behaviors.ellipsisobject.DeprecatedUserInfo
import models.behaviors.conversations.conversation.Conversation
import models.behaviors.events._
import models.team.Team
import services.{DataService, DefaultServices}
import slick.dbio.DBIO

import scala.concurrent.ExecutionContext

trait TestEvent extends Event {
  val user: User
  val team: Team

  val eventType: EventType = EventType.test
  val maybeOriginalEventType: Option[EventType] = None
  def withOriginalEventType(originalEventType: EventType, isUninterrupted: Boolean): Event = this

  val botUserIdForContext: String = "TEST_BOT_ID"

  def isDirectMessage(channel: String): Boolean = false

  override def deprecatedUserInfoAction(
                               maybeConversation: Option[Conversation],
                               services: DefaultServices
                             )(implicit actorSystem: ActorSystem, ec: ExecutionContext): DBIO[DeprecatedUserInfo] = {
    DeprecatedUserInfo.buildForAction(user, this, maybeConversation, services)
  }

  override def ensureUserAction(dataService: DataService): DBIO[User] = {
    DBIO.successful(user)
  }

}

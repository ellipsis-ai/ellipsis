package models.behaviors.testing

import akka.actor.ActorSystem
import models.accounts.user.User
import models.behaviors.behaviorversion.BehaviorResponseType
import models.behaviors.conversations.conversation.Conversation
import models.behaviors.events._
import models.behaviors.{ActionChoice, DeveloperContext, UserInfo}
import models.team.Team
import play.api.Configuration
import play.api.libs.json.JsObject
import services.{DataService, DefaultServices}
import slick.dbio.DBIO
import utils.UploadFileSpec

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.{ExecutionContext, Future}

trait TestEvent extends Event {
  val user: User
  val team: Team

  val eventType: EventType = EventType.test
  val maybeOriginalEventType: Option[EventType] = None
  def withOriginalEventType(originalEventType: EventType, isUninterrupted: Boolean): Event = this

  val botUserIdForContext: String = "TEST_BOT_ID"
  def messageUserDataList: Set[MessageUserData] = Set.empty

  val isResponseExpected = true

  def isDirectMessage(channel: String): Boolean = false

  override def userInfoAction(
                               maybeConversation: Option[Conversation],
                               services: DefaultServices
                             )(implicit actorSystem: ActorSystem, ec: ExecutionContext): DBIO[UserInfo] = {
    UserInfo.buildForAction(user, this, maybeConversation, services)
  }

  override def ensureUserAction(dataService: DataService): DBIO[User] = {
    DBIO.successful(user)
  }

}

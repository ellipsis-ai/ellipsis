package models.behaviors.testing

import akka.actor.ActorSystem
import models.accounts.user.User
import models.behaviors.{ActionChoice, DeveloperContext, UserInfo}
import models.behaviors.conversations.conversation.Conversation
import models.behaviors.events._
import models.team.Team
import play.api.Configuration
import play.api.libs.json.JsObject
import services.caching.CacheService
import services.{DataService, DefaultServices}
import slick.dbio.DBIO
import utils.UploadFileSpec

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.{ExecutionContext, Future}

case class TestEvent(
                      user: User,
                      team: Team,
                      messageText: String,
                      includesBotMention: Boolean
                    ) extends MessageEvent {

  val eventType: EventType = EventType.test
  val maybeOriginalEventType: Option[EventType] = None
  def withOriginalEventType(originalEventType: EventType, isUninterrupted: Boolean): Event = this

  val teamId = team.id

  val messageBuffer: ArrayBuffer[String] = new ArrayBuffer()

  lazy val userIdForContext = user.id
  lazy val name = "test"
  lazy val maybeChannel = Some("C123456")
  lazy val maybeThreadId = None
  def eventualMaybeDMChannel(cacheService: CacheService)(implicit actorSystem: ActorSystem, ec: ExecutionContext) = Future.successful(None)
  def maybeChannelForSendAction(
                                 forcePrivate: Boolean,
                                 maybeConversation: Option[Conversation],
                                 services: DefaultServices
                               )(implicit ec: ExecutionContext, actorSystem: ActorSystem): DBIO[Option[String]] = DBIO.successful(None)
  val isResponseExpected = true
  val messageRecipientPrefix: String = ""
  lazy val isPublicChannel = false

  def isDirectMessage(channel: String): Boolean = false

  def sendMessage(
                   text: String,
                   forcePrivate: Boolean,
                   maybeShouldUnfurl: Option[Boolean],
                   maybeConversation: Option[Conversation],
                   attachmentGroups: Seq[MessageAttachmentGroup],
                   files: Seq[UploadFileSpec],
                   choices: Seq[ActionChoice],
                   developerContext: DeveloperContext,
                   services: DefaultServices,
                   configuration: Configuration
                 )(implicit actorSystem: ActorSystem, ec: ExecutionContext): Future[Option[String]] = {
    Future.successful(messageBuffer += text).map(_ => None)
  }

  override def userInfoAction(
                               services: DefaultServices
                             )(implicit actorSystem: ActorSystem, ec: ExecutionContext): DBIO[UserInfo] = {
    UserInfo.buildForAction(user, this, services)
  }

  override def ensureUserAction(dataService: DataService): DBIO[User] = {
    DBIO.successful(user)
  }

  def detailsFor(services: DefaultServices)(implicit actorSystem: ActorSystem, ec: ExecutionContext): Future[JsObject] = {
    Future.successful(JsObject(Seq()))
  }

}

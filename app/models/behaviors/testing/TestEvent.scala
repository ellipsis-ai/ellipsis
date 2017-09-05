package models.behaviors.testing

import akka.actor.ActorSystem
import models.accounts.user.User
import models.behaviors.UserInfo
import models.behaviors.conversations.conversation.Conversation
import models.behaviors.events.{MessageActions, MessageEvent}
import models.team.Team
import play.api.libs.json.JsObject
import play.api.libs.ws.WSClient
import services.{CacheService, DataService}
import slick.dbio.DBIO
import utils.UploadFileSpec

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class TestEvent(
                      user: User,
                      team: Team,
                      messageText: String,
                      includesBotMention: Boolean
                    ) extends MessageEvent {

  val teamId = team.id

  val messageBuffer: ArrayBuffer[String] = new ArrayBuffer()

  lazy val userIdForContext = user.id
  lazy val name = "test"
  lazy val maybeChannel = None
  lazy val maybeThreadId = None
  def eventualMaybeDMChannel(cacheService: CacheService)(implicit actorSystem: ActorSystem) = Future.successful(None)
  val isResponseExpected = true
  val messageRecipientPrefix: String = ""
  lazy val isPublicChannel = false

  def isDirectMessage(channel: String): Boolean = false

  def sendMessage(
                   text: String,
                   forcePrivate: Boolean,
                   maybeShouldUnfurl: Option[Boolean],
                   maybeConversation: Option[Conversation],
                   maybeActions: Option[MessageActions],
                   files: Seq[UploadFileSpec],
                   cacheService: CacheService
                 )(implicit actorSystem: ActorSystem): Future[Option[String]] = {
    Future.successful(messageBuffer += text).map(_ => None)
  }

  override def userInfoAction(ws: WSClient, dataService: DataService, cacheService: CacheService)(implicit actorSystem: ActorSystem): DBIO[UserInfo] = {
    UserInfo.buildForAction(user, this, ws, dataService, cacheService)
  }

  override def ensureUserAction(dataService: DataService): DBIO[User] = {
    DBIO.successful(user)
  }

  def detailsFor(ws: WSClient, cacheService: CacheService)(implicit actorSystem: ActorSystem): Future[JsObject] = {
    Future.successful(JsObject(Seq()))
  }

}

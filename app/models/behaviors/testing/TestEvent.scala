package models.behaviors.testing

import models.accounts.user.User
import models.behaviors.UserInfo
import models.behaviors.conversations.conversation.Conversation
import models.behaviors.events.{MessageActions, MessageEvent}
import models.team.Team
import play.api.libs.ws.WSClient
import services.DataService

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.{ExecutionContext, Future}

case class TestEvent(
                      user: User,
                      team: Team,
                      fullMessageText: String,
                      includesBotMention: Boolean
                    ) extends MessageEvent {

  val teamId = team.id

  val messageBuffer: ArrayBuffer[String] = new ArrayBuffer()

  override def relevantMessageText: String = fullMessageText

  lazy val userIdForContext = user.id
  lazy val name = "test"
  lazy val maybeChannel = None
  lazy val maybeThreadId = None
  lazy val eventualMaybeDMChannel = Future.successful(None)
  val isResponseExpected = true

  def isDirectMessage(channel: String): Boolean = false

  def sendMessage(
                   text: String,
                   forcePrivate: Boolean,
                   maybeShouldUnfurl: Option[Boolean],
                   maybeConversation: Option[Conversation],
                   maybeActions: Option[MessageActions]
                 )(implicit ec: ExecutionContext): Future[Unit] = {
    Future.successful(messageBuffer += text)
  }

  override def userInfo(ws: WSClient, dataService: DataService): Future[UserInfo] = {
    UserInfo.buildFor(Some(user), this, ws, dataService)
  }

  override def ensureUser(dataService: DataService)(implicit ec: ExecutionContext): Future[User] = {
    Future.successful(user)
  }

}

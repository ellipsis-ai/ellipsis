package models.behaviors.testing

import models.accounts.user.User
import models.behaviors.UserInfo
import models.behaviors.conversations.conversation.Conversation
import models.behaviors.events.MessageContext
import models.team.Team
import play.api.libs.ws.WSClient
import services.DataService

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.{ExecutionContext, Future}

case class TestMessageContext(
                               user: User,
                               team: Team,
                               fullMessageText: String,
                               includesBotMention: Boolean
                             ) extends MessageContext {

  val teamId = team.id

  val messageBuffer: ArrayBuffer[String] = new ArrayBuffer()
  override def relevantMessageText: String = fullMessageText
  val userIdForContext = user.id
  val name = "test"
  val maybeChannel = None
  val isResponseExpected = true

  def maybeOngoingConversation(dataService: DataService): Future[Option[Conversation]] = Future.successful(None)

  def sendMessage(
                   text: String,
                   forcePrivate: Boolean,
                   maybeShouldUnfurl: Option[Boolean],
                   maybeConversation: Option[Conversation]
                 )(implicit ec: ExecutionContext): Unit = {
    messageBuffer += text
  }

  override def userInfo(ws: WSClient, dataService: DataService): Future[UserInfo] = {
    UserInfo.buildFor(Some(user), this, ws, dataService)
  }

  override def ensureUser(dataService: DataService)(implicit ec: ExecutionContext): Future[User] = {
    Future.successful(user)
  }

}

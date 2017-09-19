package models.behaviors.events

import akka.actor.ActorSystem
import models.behaviors.behavior.Behavior
import models.behaviors.conversations.conversation.Conversation
import models.behaviors.scheduling.Scheduled
import models.team.Team
import play.api.libs.json.JsObject
import play.api.libs.ws.WSClient
import services.{CacheService, DataService, DefaultServices}
import utils.UploadFileSpec

import scala.concurrent.{ExecutionContext, Future}

case class ScheduledEvent(underlying: Event, scheduled: Scheduled) extends Event {

  def eventualMaybeDMChannel(cacheService: CacheService)(implicit actorSystem: ActorSystem, ec: ExecutionContext): Future[Option[String]] = {
    underlying.eventualMaybeDMChannel(cacheService)
  }

  def sendMessage(
                   text: String,
                   forcePrivate: Boolean,
                   maybeShouldUnfurl: Option[Boolean],
                   maybeConversation: Option[Conversation],
                   maybeActions: Option[MessageActions],
                   files: Seq[UploadFileSpec],
                   cacheService: CacheService
                 )(implicit actorSystem: ActorSystem, ec: ExecutionContext): Future[Option[String]] = {
    underlying.sendMessage(text, forcePrivate, maybeShouldUnfurl, maybeConversation, maybeActions, files, cacheService)
  }

  override def detailsFor(ws: WSClient, dataService: DataService, cacheService: CacheService)(implicit actorSystem: ActorSystem, ec: ExecutionContext): Future[JsObject] = {
    underlying.detailsFor(ws, dataService, cacheService)
  }

  lazy val maybeChannel: Option[String] = underlying.maybeChannel
  lazy val maybeThreadId: Option[String] = underlying.maybeThreadId
  lazy val teamId: String = underlying.teamId
  lazy val name: String = underlying.name
  lazy val includesBotMention: Boolean = underlying.includesBotMention
  lazy val messageText: String = underlying.messageText
  lazy val invocationLogText: String = underlying.invocationLogText
  lazy val isResponseExpected: Boolean = underlying.isResponseExpected
  lazy val userIdForContext: String = underlying.userIdForContext
  lazy val messageRecipientPrefix: String = underlying.messageRecipientPrefix
  override val maybeScheduled: Option[Scheduled] = Some(scheduled)
  lazy val isPublicChannel: Boolean = underlying.isPublicChannel

  def allBehaviorResponsesFor(
                               maybeTeam: Option[Team],
                               maybeLimitToBehavior: Option[Behavior],
                               services: DefaultServices
                             )(implicit ec: ExecutionContext) = underlying.allBehaviorResponsesFor(maybeTeam, maybeLimitToBehavior, services)

  def allOngoingConversations(dataService: DataService) = underlying.allOngoingConversations(dataService)

}

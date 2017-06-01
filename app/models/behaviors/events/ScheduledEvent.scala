package models.behaviors.events

import akka.actor.ActorSystem
import models.behaviors.behavior.Behavior
import models.behaviors.conversations.conversation.Conversation
import models.behaviors.scheduling.Scheduled
import models.team.Team
import play.api.Configuration
import play.api.cache.CacheApi
import play.api.libs.ws.WSClient
import services.{AWSLambdaService, DataService}

import scala.concurrent.Future

case class ScheduledEvent(underlying: Event, scheduled: Scheduled) extends Event {

  def eventualMaybeDMChannel(dataService: DataService)(implicit actorSystem: ActorSystem): Future[Option[String]] = {
    underlying.eventualMaybeDMChannel(dataService)
  }

  def sendMessage(
                   text: String,
                   forcePrivate: Boolean,
                   maybeShouldUnfurl: Option[Boolean],
                   maybeConversation: Option[Conversation],
                   maybeActions: Option[MessageActions],
                   dataService: DataService
                 )(implicit actorSystem: ActorSystem) = {
    underlying.sendMessage(text, forcePrivate, maybeShouldUnfurl, maybeConversation, maybeActions, dataService)
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
                               lambdaService: AWSLambdaService,
                               dataService: DataService,
                               cache: CacheApi,
                               ws: WSClient,
                               configuration: Configuration,
                               actorSystem: ActorSystem
                             ) = underlying.allBehaviorResponsesFor(maybeTeam, maybeLimitToBehavior, lambdaService, dataService, cache, ws, configuration, actorSystem)

  def allOngoingConversations(dataService: DataService) = underlying.allOngoingConversations(dataService)

}

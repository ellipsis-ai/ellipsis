package models.behaviors.events.ms_teams

import akka.actor.ActorSystem
import models.accounts.ms_teams.botprofile.MSTeamsBotProfile
import models.behaviors.conversations.conversation.Conversation
import models.behaviors.events._
import services.{DataService, DefaultServices}

import scala.concurrent.{ExecutionContext, Future}

case class MSTeamsMessageEvent(
                                eventContext: MSTeamsEventContext,
                                message: String,
                                maybeOriginalEventType: Option[EventType],
                                override val isUninterruptedConversation: Boolean,
                                override val isEphemeral: Boolean,
                                override val maybeResponseUrl: Option[String],
                                override val beQuiet: Boolean
                              ) extends MessageEvent {

  override type EC = MSTeamsEventContext

  val profile: MSTeamsBotProfile = eventContext.profile
  val channel: String = eventContext.channel
  val user: String = eventContext.userIdForContext

  val eventType: EventType = EventType.chat

  val maybeMessageIdForReaction: Option[String] = None // TODO: populate this

  override def maybePermalinkFor(services: DefaultServices)(implicit actorSystem: ActorSystem, ec: ExecutionContext): Future[Option[String]] = {
    Future.successful(None) // TODO: maybe implement this
    // eventContext.maybePermalinkFor(ts, services)
  }

  def withOriginalEventType(originalEventType: EventType, isUninterrupted: Boolean): Event = {
    this.copy(maybeOriginalEventType = Some(originalEventType), isUninterruptedConversation = isUninterrupted)
  }

  override val isBotMessage: Boolean = false // TODO: check this

  override def contextualBotPrefix(services: DefaultServices)(implicit actorSystem: ActorSystem, ec: ExecutionContext): Future[String] = {
    if (eventContext.isDirectMessage) {
      Future.successful("")
    } else {
      botName(services).map(n => s"@$n ")
    }
  }

  val messageText: String = message

  val botMentionRegex = s"""<at>${eventContext.info.recipient.name}</at>"""

  override val relevantMessageText: String = {
    message.replaceAll(botMentionRegex, "").trim
  }

  override val relevantMessageTextWithFormatting: String = {
    relevantMessageText
  }

  lazy val includesBotMention: Boolean = true

  def messageUserDataList: Set[MessageUserData] = {
    Set()
    // TODO: look at this
//    message.userList.map(MessageUserData.fromSlackUserData)
  }

  override val isResponseExpected: Boolean = includesBotMention

  override def maybeOngoingConversation(dataService: DataService)(implicit ec: ExecutionContext): Future[Option[Conversation]] = {
    dataService.conversations.findOngoingFor(user, eventContext.name, maybeChannel, maybeThreadId, ellipsisTeamId).flatMap { maybeConvo =>
      maybeConvo.map(c => Future.successful(Some(c))).getOrElse(maybeConversationRootedHere(dataService))
    }
  }

  def maybeConversationRootedHere(dataService: DataService): Future[Option[Conversation]] = {
    dataService.conversations.findOngoingFor(user, eventContext.name, maybeChannel, None, ellipsisTeamId)
  }

}
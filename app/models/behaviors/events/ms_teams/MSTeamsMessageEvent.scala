package models.behaviors.events.ms_teams

import akka.actor.ActorSystem
import json.UserData
import models.accounts.ms_teams.botprofile.MSTeamsBotProfile
import models.behaviors.conversations.conversation.Conversation
import models.behaviors.events._
import models.behaviors.scheduling.Scheduled
import services.ms_teams.apiModels.{Attachment, File, Image}
import services.{DataService, DefaultServices}
import slick.dbio.DBIO
import utils.FileReference

import scala.concurrent.{ExecutionContext, Future}

case class MSTeamsMessageEvent(
                                eventContext: MSTeamsEventContext,
                                message: String,
                                attachments: Seq[Attachment],
                                maybeOriginalEventType: Option[EventType],
                                maybeScheduled: Option[Scheduled],
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

  val maybeFile: Option[FileReference] = attachments.flatMap {
    case Attachment(_, Some(f: File), _, _) => Some(f)
    case Attachment("image/*", None, Some(url: String), _) => Some(Image(url, Some(url)))
    case _ => None
  }.headOption

  val maybeMessageId: Option[String] = None // TODO: populate this

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

  val botMentionRegex = s"""(?:<at>${eventContext.info.botParticipant.name}</at>)*(?:<span itemscope="" itemtype="http://schema.skype.com/Mention" itemid="\\d+">${eventContext.info.botParticipant.name}</span>)*"""

  override val relevantMessageText: String = {
    message.replaceAll(botMentionRegex, "").trim
  }

  override val relevantMessageTextWithFormatting: String = {
    relevantMessageText
  }

  lazy val includesBotMention: Boolean = true

  def messageUserDataListAction(services: DefaultServices)(implicit ec: ExecutionContext): DBIO[Set[UserData]] = {
    DBIO.successful(Set())
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

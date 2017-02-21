package models.behaviors.events

import akka.actor.ActorSystem
import models.accounts.slack.botprofile.SlackBotProfile
import models.behaviors.behavior.Behavior
import models.behaviors.conversations.conversation.Conversation
import services.DataService
import utils.SlackMessageSender

import scala.concurrent.Future

case class RunEvent(
                    profile: SlackBotProfile,
                    behavior: Behavior,
                    channel: String,
                    maybeThreadId: Option[String],
                    user: String,
                    ts: String
                  ) extends Event with SlackEvent {

  val messageText: String = ""
  val includesBotMention: Boolean = false
  val isResponseExpected: Boolean = false
  val invocationLogText: String = s"Running behavior ${behavior.id}"

  val teamId: String = behavior.team.id
  val userIdForContext: String = user

  lazy val maybeChannel = Some(channel)
  lazy val name: String = Conversation.SLACK_CONTEXT

  def allOngoingConversations(dataService: DataService): Future[Seq[Conversation]] = Future.successful(Seq())

  def sendMessage(
                   unformattedText: String,
                   forcePrivate: Boolean,
                   maybeShouldUnfurl: Option[Boolean],
                   maybeConversation: Option[Conversation],
                   maybeActions: Option[MessageActions] = None
                 )(implicit actorSystem: ActorSystem): Future[Option[String]] = {
    SlackMessageSender(
      client,
      user,
      unformattedText,
      forcePrivate,
      channel,
      channel,
      maybeThreadId,
      maybeShouldUnfurl,
      maybeConversation,
      maybeActions
    ).send
  }

}

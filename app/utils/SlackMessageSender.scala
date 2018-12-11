package utils

import akka.actor.ActorSystem
import json.Formatting._
import models.SlackMessageFormatter
import models.behaviors.behaviorversion.{BehaviorResponseType, Private}
import models.behaviors.conversations.conversation.Conversation
import models.behaviors.events.MessageActionConstants._
import models.behaviors.events.{slack, _}
import models.behaviors.events.slack._
import models.behaviors.{ActionChoice, DeveloperContext}
import play.api.Configuration
import play.api.libs.json.Json
import services.DefaultServices
import services.slack.{SlackApiClient, SlackApiError}
import services.slack.apiModels._

import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.io.File

trait SlackMessageSenderChannelException extends Exception {
  val channel: String
  val channelLink: String = if (channel.startsWith("#")) {
    channel
  } else {
    s"<#$channel>"
  }
  val slackTeamId: String
  val userId: String
  val text: String
  protected def channelReason(channelIdOrLink: String): String
  def formattedChannelReason = channelReason(channelLink)
  def rawChannelReason = channelReason(channel)
  override def getMessage: String = {
    s"""Could not send to channel ID $channel while sending a message to user $userId on team $slackTeamId. $rawChannelReason
       |
       |Message:
       |$text
       |""".stripMargin
  }
}

case class ArchivedChannelException(channel: String, slackTeamId: String, userId: String, text: String) extends SlackMessageSenderChannelException {
  def channelReason(channelIdOrLink: String) = s"The channel ${channelIdOrLink} has been archived."
}

case class NotInvitedToChannelException(channel: String, slackTeamId: String, userId: String, text: String) extends SlackMessageSenderChannelException {
  def channelReason(channelIdOrLink: String) = s"The bot needs to be invited to the channel ${channelIdOrLink}."
}

case class ChannelNotFoundException(channel: String, slackTeamId: String, userId: String, text: String) extends SlackMessageSenderChannelException {
  def channelReason(channelIdOrLink: String) = s"The channel could not be found. It may be private and the bot is not a member, or it may no longer exist."
}

case class RestrictedFromChannel(channel: String, slackTeamId: String, userId: String, text: String) extends SlackMessageSenderChannelException {
  def channelReason(channelIdOrLink: String) = s"The bot is restricted from posting to the channel ${channelIdOrLink} by the admin."
}

case class SlackMessageSenderException(underlying: Throwable, channel: String, slackTeamId: String, userId: String, text: String)
  extends Exception(
    s"""Bad response from Slack while sending a message to user $userId in channel $channel on team $slackTeamId
       |Message:
       |$text
       |
       |Underlying cause:
       |${underlying.toString}
     """.stripMargin, underlying) {
}

case class SlackMessageSender(
                               client: SlackApiClient,
                               user: String,
                               slackTeamId: String,
                               unformattedText: String,
                               responseType: BehaviorResponseType,
                               developerContext: DeveloperContext,
                               originatingChannel: String,
                               maybeDMChannel: Option[String],
                               maybeThreadId: Option[String],
                               maybeShouldUnfurl: Option[Boolean],
                               maybeConversation: Option[Conversation],
                               attachments: Seq[MessageAttachment] = Seq(),
                               files: Seq[UploadFileSpec] = Seq(),
                               choices: Seq[ActionChoice],
                               configuration: Configuration,
                               botName: String,
                               userDataList: Set[EventUserData],
                               services: DefaultServices,
                               isEphemeral: Boolean,
                               maybeResponseUrl: Option[String],
                               beQuiet: Boolean
                             ) {

  val choicesAttachments: Seq[SlackMessageAttachment] = {
    if (choices.isEmpty) {
      Seq()
    } else {
      val actionList = choices.zipWithIndex.map { case(ea, i) =>
        val value = Json.toJson(ea).toString()
        val valueToUse = if (value.length > SlackMessageSender.MAX_ACTION_VALUE_CHARS) {
          services.cacheService.cacheSlackActionValue(value)
        } else {
          value
        }
        SlackMessageActionButton(ACTION_CHOICE, ea.label, valueToUse)
      }
      Seq(slack.SlackMessageAttachment(
        None,
        None,
        None,
        None,
        Some(Color.BLUE_LIGHTER),
        Some(ACTION_CHOICES),
        actionList
      ))
    }
  }

  val attachmentsToUse = {
    val groups = attachments ++ choicesAttachments
    if (developerContext.isForUndeployedBehaviorVersion) {
      val baseUrl = configuration.get[String]("application.apiBaseUrl")
      val path = controllers.routes.HelpController.devMode(Some(slackTeamId), Some(botName)).url
      val link = s"[development]($baseUrl$path)"
      groups ++ Seq(SlackMessageAttachment(Some(s"\uD83D\uDEA7 Skill in $link \uD83D\uDEA7"), None, None))
    } else if (developerContext.hasUndeployedBehaviorVersionForAuthor) {
      val baseUrl = configuration.get[String]("application.apiBaseUrl")
      val path = controllers.routes.HelpController.devMode(Some(slackTeamId), Some(botName)).url
      val link = s"[dev mode]($baseUrl$path)"
      groups ++ Seq(
        SlackMessageAttachment(
          Some(s"\uD83D\uDEA7 You are running the deployed version of this skill even though you've made changes. You can always use the most recent version in $link."),
          None,
          None
        )
      )
    } else {
      groups
    }
  }

  private def postEphemeralMessage(
                                    text: String,
                                    maybeAttachments: Option[Seq[Attachment]] = None,
                                    channel: String,
                                    maybeThreadTs: Option[String]
                                  )(implicit actorSystem: ActorSystem, ec: ExecutionContext): Future[String] = {
    client.postEphemeralMessage(
      text,
      channel,
      maybeThreadTs,
      user,
      asUser = Some(false), // allows it to work in channels where bot is not a member
      parse = None,
      linkNames = None,
      attachments = maybeAttachments
    ).recover(postErrorRecovery(channel, text))
  }

  private def maybeResponseUrlToUse(channel: String): Option[String] = {
    // We can't always use the response url, as it's a bit quirky:
    // - using the response url in a thread results in a message back in the main channel, for <%= reason %>
    // - posts to the response url don't give back a message ts
    if (channel == originatingChannel && maybeThreadId.isEmpty && isEphemeral) {
      maybeResponseUrl
    } else {
      None
    }
  }

  private def maybeThreadTsToUse(channel: String) = {
    responseType.maybeThreadTsToUseFor(channel, originatingChannel, maybeConversation, maybeThreadId)
  }

  private def channelToUse(maybeChannelToForce: Option[String] = None): String = {
    maybeChannelToForce.getOrElse {
      responseType.channelToUseFor(originatingChannel, maybeConversation, maybeThreadId, maybeDMChannel)
    }
  }

  private def postChatMessage(
                               text: String,
                               maybeAttachments: Option[Seq[Attachment]] = None,
                               maybeChannelToForce: Option[String] = None
                             )(implicit actorSystem: ActorSystem, ec: ExecutionContext): Future[String] = {
    val channel = channelToUse(maybeChannelToForce)
    val maybeThreadTs = maybeThreadTsToUse(channel)
    maybeResponseUrlToUse(channel).map { responseUrl =>
      client.postToResponseUrl(text, maybeAttachments, responseUrl, isEphemeral)
    }.getOrElse {
      if (isEphemeral) {
        postEphemeralMessage(text, maybeAttachments, channel, maybeThreadTs)
      } else {
        client.postChatMessage(
          channel,
          text,
          username = None,
          asUser = Some(true),
          parse = None,
          linkNames = None,
          attachments = maybeAttachments,
          unfurlLinks = Some(maybeShouldUnfurl.getOrElse(false)),
          unfurlMedia = Some(true),
          iconUrl = None,
          iconEmoji = None,
          replaceOriginal = None,
          deleteOriginal = None,
          threadTs = maybeThreadTs,
          replyBroadcast = None
        ).recover(postErrorRecovery(channel, text))
      }
    }
  }

  private def messageSegmentsFor(formattedText: String): List[String] = {
    if (formattedText.length < SlackMessageSender.MAX_MESSAGE_LENGTH) {
      List(formattedText)
    } else {
      val largestPossibleSegment = formattedText.substring(0, SlackMessageSender.MAX_MESSAGE_LENGTH)
      val lastNewlineIndex = Math.max(largestPossibleSegment.lastIndexOf('\n'), largestPossibleSegment.lastIndexOf('\r'))
      val lastIndex = if (lastNewlineIndex < 0) { SlackMessageSender.MAX_MESSAGE_LENGTH - 1 } else { lastNewlineIndex }
      (formattedText.substring(0, lastIndex)) :: messageSegmentsFor(formattedText.substring(lastIndex + 1))
    }
  }

  private def maybePreambleText: Option[String] = {
    if (responseType == Private && !maybeDMChannel.contains(originatingChannel)) {
      Some(s"<@${user}> I’ve sent you a <${client.profile.botDMDeepLink}|private message> :sleuth_or_spy:")
    } else {
      None
    }
  }

  def sendPreamble(formattedText: String)(implicit actorSystem: ActorSystem, ec: ExecutionContext): Future[Unit] = {
    if (formattedText.nonEmpty) {
      maybePreambleText.map { preambleMessage =>
        if (beQuiet) {
          postEphemeralMessage(preambleMessage, None, originatingChannel, maybeThreadId)
        } else {
          postChatMessage(preambleMessage, None, Some(originatingChannel))
        }
      }.getOrElse(Future.successful(None)).map(_ => ())
    } else {
      Future.successful({})
    }
  }

  def sendMessageSegmentsInOrder(
                                  segments: List[String],
                                  channelToUse: String,
                                  maybeShouldUnfurl: Option[Boolean],
                                  attachments: Seq[Attachment],
                                  maybeConversation: Option[Conversation],
                                  maybePreviousTs: Option[String]
                                )(implicit actorSystem: ActorSystem, ec: ExecutionContext): Future[Option[String]] = {
    if (segments.isEmpty) {
      Future.successful(maybePreviousTs)
    } else {
      val segment = segments.head.trim
      // Slack API gives an error for empty messages
      if (segment.isEmpty && attachments.isEmpty) {
        Future.successful(None)
      } else {
        val maybeAttachmentsForSegment = if (segments.tail.isEmpty) {
          Some(attachments).filter(_.nonEmpty)
        } else {
          None
        }

        postChatMessage(
          segment,
          maybeAttachmentsForSegment
        )
      }.flatMap { ts => sendMessageSegmentsInOrder(segments.tail, channelToUse, maybeShouldUnfurl, attachments, maybeConversation, Some(ts))}
    }
  }

  def sendFile(spec: UploadFileSpec)(implicit actorSystem: ActorSystem, ec: ExecutionContext): Future[Unit] = {
    val file = File.makeTemp().jfile
    val channel = channelToUse()
    client.uploadFile(
      file,
      content = spec.content,
      filetype = spec.filetype,
      filename = spec.filename,
      channels = Some(Seq(channel)),
      maybeThreadTs = maybeThreadTsToUse(channel)
    ).map(_ => {})
  }

  def sendFiles(implicit actorSystem: ActorSystem, ec: ExecutionContext): Future[Unit] = {
    Future.sequence(files.map(sendFile)).map(_ => {})
  }

  def send(implicit actorSystem: ActorSystem, ec: ExecutionContext): Future[Option[String]] = {
    val formattedText = SlackMessageFormatter.bodyTextFor(unformattedText, userDataList)
    val attachments = attachmentsToUse.flatMap {
      case a: SlackMessageAttachment => Some(a.underlying)
      case _ => None
    }
    for {
      _ <- sendPreamble(formattedText)
      maybeLastTs <- sendMessageSegmentsInOrder(messageSegmentsFor(formattedText), originatingChannel, maybeShouldUnfurl, attachments, maybeConversation, None)
      _ <- sendFiles
    } yield maybeLastTs
  }

  private def postErrorRecovery[U](channel: String, text: String): PartialFunction[Throwable, U] = {
    case SlackApiError("is_archived") => throw ArchivedChannelException(channel, slackTeamId, user, text)
    case SlackApiError("channel_not_found") => throw ChannelNotFoundException(channel, slackTeamId, user, text)
    case SlackApiError("not_in_channel") => throw NotInvitedToChannelException(channel, slackTeamId, user, text)
    case SlackApiError("restricted_action") => throw RestrictedFromChannel(channel, slackTeamId, user, text)
    case t: Throwable => throw SlackMessageSenderException(t, channel, slackTeamId, user, text)
  }
}

object SlackMessageSender {
  // From Slack docs:
  //
  // "For best results, message bodies should contain no more than a few thousand characters."
  val MAX_MESSAGE_LENGTH = 2000
  // "A maximum of 5 actions may be provided."
  val MAX_ACTIONS_PER_ATTACHMENT = 5
  // "Your value may contain up to 2000 characters."
  val MAX_ACTION_VALUE_CHARS = 2000
}

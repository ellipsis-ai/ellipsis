package utils

import akka.actor.ActorSystem
import models.SlackMessageFormatter
import models.behaviors.conversations.conversation.Conversation
import models.behaviors.events.{MessageAttachmentGroup, SlackMessageAttachmentGroup}
import slack.api.SlackApiClient
import slack.models.Attachment

import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.io.File

case class SlackMessageSender(
                               client: SlackApiClient,
                               user: String,
                               unformattedText: String,
                               forcePrivate: Boolean,
                               originatingChannel: String,
                               channelToUse: String,
                               maybeThreadId: Option[String],
                               maybeShouldUnfurl: Option[Boolean],
                               maybeConversation: Option[Conversation],
                               attachmentGroups: Seq[MessageAttachmentGroup] = Seq(),
                               files: Seq[UploadFileSpec] = Seq()
                             ) {

  private def postChatMessage(
                               text: String,
                               maybeThreadTs: Option[String] = None,
                               maybeReplyBroadcast: Option[Boolean] = None,
                               maybeAttachments: Option[Seq[Attachment]] = None,
                               maybeChannelToForce: Option[String] = None
                             )(implicit actorSystem: ActorSystem): Future[String] = {
    client.postChatMessage(
      maybeChannelToForce.getOrElse(maybeThreadTs.map(_ => originatingChannel).getOrElse(channelToUse)),
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
      replyBroadcast = maybeReplyBroadcast
    )
  }

  private def isDirectMessage(channelId: String): Boolean = {
    channelId.startsWith("D")
  }

  private def isPrivateChannel(channelId: String): Boolean = {
    channelId.startsWith("G")
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

  def sendPreamble(formattedText: String, channelToUse: String)(implicit actorSystem: ActorSystem, ec: ExecutionContext): Future[Unit] = {
    if (formattedText.nonEmpty) {
      for {
        _ <- if (maybeThreadId.isDefined && maybeConversation.flatMap(_.maybeThreadId).isEmpty) {
          val channelText = if (isDirectMessage(originatingChannel)) {
            "the DM channel"
          } else if (isPrivateChannel(originatingChannel)) {
            "the private channel"
          } else {
            s"<#$originatingChannel>"
          }
          postChatMessage(
            text = s"<@${user}> I've responded back in $channelText.",
            maybeThreadTs = maybeThreadId
          )
        } else {
          Future.successful({})
        }
        _ <- if (isDirectMessage(channelToUse) && channelToUse != originatingChannel) {
          postChatMessage(
            s"<@${user}> I've sent you a private message :sleuth_or_spy:",
            maybeChannelToForce = Some(originatingChannel)
          )
        } else {
          Future.successful({})
        }
        _ <- if (!isDirectMessage(channelToUse) && channelToUse != originatingChannel) {
          postChatMessage(s"<@${user}> OK, back to <#${channelToUse}>")
        } else {
          Future.successful({})
        }
      } yield {}
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
      if (segment.isEmpty) {
        Future.successful(None)
      } else {
        val maybeAttachmentsForSegment = if (segments.tail.isEmpty) {
          Some(attachments).filter(_.nonEmpty)
        } else {
          None
        }
        val maybeThreadTsToUse = maybeConversation.flatMap(_.maybeThreadId)

        postChatMessage(
          segment,
          maybeThreadTsToUse,
          maybeReplyBroadcast = Some(false),
          maybeAttachmentsForSegment
        )
      }.flatMap { ts => sendMessageSegmentsInOrder(segments.tail, channelToUse, maybeShouldUnfurl, attachments, maybeConversation, Some(ts))}
    }
  }

  def sendFile(spec: UploadFileSpec)(implicit actorSystem: ActorSystem, ec: ExecutionContext): Future[Unit] = {
    val file = File.makeTemp().jfile
    client.uploadFile(
      file,
      content = spec.content,
      filetype = spec.filetype,
      filename = spec.filename,
      channels = Some(Seq(channelToUse))
    ).map(_ => {})
  }

  def sendFiles(implicit actorSystem: ActorSystem, ec: ExecutionContext): Future[Unit] = {
    Future.sequence(files.map(sendFile)).map(_ => {})
  }

  def send(implicit actorSystem: ActorSystem, ec: ExecutionContext): Future[Option[String]] = {
    val formattedText = SlackMessageFormatter.bodyTextFor(unformattedText)
    val attachments = attachmentGroups.flatMap {
      case a: SlackMessageAttachmentGroup => a.attachments.map(_.underlying)
      case _ => Seq()
    }
    for {
      _ <- sendPreamble(formattedText, channelToUse)
      maybeLastTs <- sendMessageSegmentsInOrder(messageSegmentsFor(formattedText), channelToUse, maybeShouldUnfurl, attachments, maybeConversation, None)
      _ <- sendFiles
    } yield maybeLastTs
  }
}

object SlackMessageSender {
  // From Slack docs:
  //
  // "For best results, message bodies should contain no more than a few thousand characters."
  val MAX_MESSAGE_LENGTH = 2000
  // "A maximum of 5 actions may be provided."
  val MAX_ACTIONS_PER_ATTACHMENT = 5
}

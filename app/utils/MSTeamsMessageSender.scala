package utils

import akka.actor.ActorSystem
import models.behaviors.behaviorversion.{BehaviorResponseType, Private}
import models.behaviors.conversations.conversation.Conversation
import models.behaviors.events._
import models.behaviors.{ActionChoice, DeveloperContext}
import play.api.libs.json.Json
import services.DefaultServices
import services.ms_teams.MSTeamsApiClient
import services.ms_teams.apiModels.{ActivityInfo, ResponseInfo, _}

import scala.concurrent.{ExecutionContext, Future}

case class MSTeamsMessageSenderException(underlying: Throwable, channel: String, teamIdForContext: String, userId: String, text: String)
  extends Exception(
    s"""Bad response from MS Teams while sending a message to user $userId in channel $channel on team $teamIdForContext
       |Message:
       |$text
       |
       |Underlying cause:
       |${underlying.toString}
     """.stripMargin, underlying) {
}

case class MSTeamsMessageSender(
                                 client: MSTeamsApiClient,
                                 user: String,
                                 teamIdForContext: String,
                                 info: ActivityInfo,
                                 unformattedText: String,
                                 responseType: BehaviorResponseType,
                                 developerContext: DeveloperContext,
                                 originatingChannel: String,
                                 maybeDMChannel: Option[String],
                                 maybeThreadId: Option[String],
                                 maybeShouldUnfurl: Option[Boolean],
                                 maybeConversation: Option[Conversation],
                                 attachmentGroups: Seq[MessageAttachmentGroup] = Seq(),
                                 files: Seq[UploadFileSpec] = Seq(),
                                 choices: Seq[ActionChoice],
                                 botName: String,
                                 userDataList: Set[MessageUserData],
                                 services: DefaultServices,
                                 isEphemeral: Boolean,
                                 beQuiet: Boolean
                             ) {

  val configuration = services.configuration

  import _root_.services.ms_teams.apiModels.Formatting._

  private def postEphemeralMessage(
                                    text: String,
                                    maybeAttachments: Option[Seq[Attachment]] = None,
                                    channel: String,
                                    maybeThreadTs: Option[String]
                                  )(implicit actorSystem: ActorSystem, ec: ExecutionContext): Future[String] = Future.successful("not a thing yet")

  private def postChatMessage(
                               text: String,
                               maybeAttachments: Option[Seq[Attachment]] = None,
                               maybeChannelToForce: Option[String] = None
                             )(implicit actorSystem: ActorSystem, ec: ExecutionContext): Future[String] = {
    val response = ResponseInfo(
      "message",
      info.recipient,
      info.conversation,
      info.from,
      unformattedText,
      "markdown",
      info.id
    )
    client.postToResponseUrl(info.responseUrl, Json.toJson(response)).recover(postErrorRecovery(info, text))
  }

  private def messageSegmentsFor(formattedText: String): List[String] = {
    if (formattedText.length < MSTeamsMessageSender.MAX_MESSAGE_LENGTH) {
      List(formattedText)
    } else {
      val largestPossibleSegment = formattedText.substring(0, MSTeamsMessageSender.MAX_MESSAGE_LENGTH)
      val lastNewlineIndex = Math.max(largestPossibleSegment.lastIndexOf('\n'), largestPossibleSegment.lastIndexOf('\r'))
      val lastIndex = if (lastNewlineIndex < 0) { MSTeamsMessageSender.MAX_MESSAGE_LENGTH - 1 } else { lastNewlineIndex }
      (formattedText.substring(0, lastIndex)) :: messageSegmentsFor(formattedText.substring(lastIndex + 1))
    }
  }

  private def maybePreambleText: Option[String] = {
    if (responseType == Private && !maybeDMChannel.contains(originatingChannel)) {
      Some(s"<@${user}> I’ve sent you a <${client.botDMDeepLink}|private message> :sleuth_or_spy:")
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
    // TODO: implement me
    Future.successful({})
  }

  def sendFiles(implicit actorSystem: ActorSystem, ec: ExecutionContext): Future[Unit] = {
    Future.sequence(files.map(sendFile)).map(_ => {})
  }

  def send(implicit actorSystem: ActorSystem, ec: ExecutionContext): Future[Option[String]] = {
    val formattedText = unformattedText // TODO: formatting
    val attachments = Seq()
    for {
      _ <- sendPreamble(formattedText)
      maybeLastTs <- sendMessageSegmentsInOrder(messageSegmentsFor(formattedText), originatingChannel, maybeShouldUnfurl, attachments, maybeConversation, None)
      _ <- sendFiles
    } yield maybeLastTs
  }

  private def postErrorRecovery[U](info: ActivityInfo, text: String): PartialFunction[Throwable, U] = {
    case t: Throwable => throw MSTeamsMessageSenderException(t, info.conversation.id, teamIdForContext, user, text)
  }
}

object MSTeamsMessageSender {

  // Guessing so far

  val MAX_MESSAGE_LENGTH = 2000
  // "A maximum of 5 actions may be provided."
  val MAX_ACTIONS_PER_ATTACHMENT = 5
  // "Your value may contain up to 2000 characters."
  val MAX_ACTION_VALUE_CHARS = 2000
}

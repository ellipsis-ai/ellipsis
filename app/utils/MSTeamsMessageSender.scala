package utils

import java.time.OffsetDateTime

import akka.actor.ActorSystem
import com.mohiva.play.silhouette.api.LoginInfo
import json.Formatting._
import json.UserData
import models.MSTeamsMessageFormatter
import models.behaviors.behaviorversion.{BehaviorResponseType, BehaviorVersion, Private}
import models.behaviors.conversations.conversation.Conversation
import models.behaviors.events.MessageActionConstants._
import models.behaviors.events._
import models.behaviors.events.ms_teams._
import models.behaviors.{ActionChoice, DeveloperContext}
import models.team.{Team => EllipsisTeam}
import play.api.libs.json.Json
import services.DefaultServices
import services.ms_teams.{MSTeamsApiClient, MSTeamsApiProfileClient, MSTeamsUser}
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

case class MSTeamsGetConversationIdException(underlying: Throwable, channel: String, teamIdForContext: String, userId: String)
  extends Exception(
    s"""Bad response from MS Teams while creating a conversation with user $userId in channel $channel on team $teamIdForContext
       |
       |Underlying cause:
       |${underlying.toString}
     """.stripMargin, underlying) {
}

case class MSTeamsMessageSender(
                                 client: MSTeamsApiClient,
                                 user: String,
                                 teamIdForContext: String,
                                 info: EventInfo,
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
                                 botName: String,
                                 services: DefaultServices,
                                 isEphemeral: Boolean,
                                 beQuiet: Boolean,
                                 maybeBehaviorVersion: Option[BehaviorVersion]
                             ) {

  import _root_.services.ms_teams.apiModels.Formatting._

  val configuration = services.configuration

  val choicesAttachments: Seq[MSTeamsMessageAttachment] = {
    if (choices.isEmpty) {
      Seq()
    } else {
      val actionList = choices.zipWithIndex.map { case(ea, i) =>
        MSTeamsMessageActionButton(ea.label, ACTION_CHOICE, Json.obj(ACTION_CHOICE -> Json.toJson(ea)))
      }
      Seq(MSTeamsMessageAttachment(
        maybeText = Some("Choose an option"),
        maybeColor = Some(Color.BLUE_LIGHTER),
        maybeCallbackId = Some(ACTION_CHOICES),
        actions = actionList
      ))
    }
  }

  val attachmentsToUse: Seq[MessageAttachment] = {
    val toUse = attachments ++ choicesAttachments
    if (developerContext.isForUndeployedBehaviorVersion) {
      val baseUrl = configuration.get[String]("application.apiBaseUrl")
      val path = controllers.routes.HelpController.devMode(Some(teamIdForContext), Some(botName)).url
      val link = s"[development]($baseUrl$path)"
      toUse ++ Seq(MSTeamsMessageAttachment(Some(s"\uD83D\uDEA7 Skill in $link \uD83D\uDEA7"), None, None))
    } else if (developerContext.hasUndeployedBehaviorVersionForAuthor) {
      val baseUrl = configuration.get[String]("application.apiBaseUrl")
      val path = controllers.routes.HelpController.devMode(Some(teamIdForContext), Some(botName)).url
      val link = s"[dev mode]($baseUrl$path)"
      toUse ++ Seq(
        MSTeamsMessageAttachment(
          Some(s"\uD83D\uDEA7 You are running the deployed version of this skill even though you've made changes. You can always use the most recent version in $link."),
          None,
          None
        )
      )
    } else {
      toUse
    }
  }

  private def postEphemeralMessage(
                                    text: String,
                                    maybeAttachments: Option[Seq[Attachment]] = None,
                                    channel: String,
                                    maybeThreadTs: Option[String]
                                  )(implicit actorSystem: ActorSystem, ec: ExecutionContext): Future[String] = Future.successful("not a thing yet")

  private def getPrivateConversationId(implicit actorSystem: ActorSystem, ec: ExecutionContext): Future[String] = {
    val response = GetPrivateConversationInfo(
      info.botParticipant,
      Seq(DirectoryObject(info.userIdForContext)),
      ChannelDataInfo(None, info.channelData.tenant, None, None)
    )
    client.postToResponseUrl(info.getPrivateConversationIdUrl, Json.toJson(response)).recover {
      case t: Throwable => throw MSTeamsGetConversationIdException(t, info.channel, teamIdForContext, user)
    }
  }

  private def getChannelMembersFor(
                                    info: EventInfo
                                  )(implicit actorSystem: ActorSystem, ec: ExecutionContext): Future[Seq[String]] = {
    if (info.isPublicChannel) {
      val maybeBotProfile = client match {
        case c: MSTeamsApiProfileClient => Some(c.profile)
        case _ => None
      }
      for {
        maybeChannel <- info.channelData.channel.map { channel =>
          maybeBotProfile.map { profile =>
            services.cacheService.getMSTeamsChannelFor(profile, channel.idWithoutMessage)
          }.getOrElse(Future.successful(None))
        }.getOrElse(Future.successful(None))
        members <- maybeChannel.map { channel =>
          client.getTeamMembers(channel.team.id)
        }.getOrElse(Future.successful(Seq()))
      } yield members.map(_.id)
    } else {
      Future.successful(info.aadObjectId.toSeq)
    }

  }

  private def logInvolvedFor(info: EventInfo)(implicit actorSystem: ActorSystem, ec: ExecutionContext): Future[Unit] = {
    maybeBehaviorVersion.map { behaviorVersion =>
      for {
        members <- getChannelMembersFor(info)
        users <- Future.sequence(members.map { ea =>
          services.dataService.users.ensureUserFor(LoginInfo(Conversation.MS_AAD_CONTEXT, ea), Seq(), behaviorVersion.team.id)
        })
        _ <- services.dataService.behaviorVersionUserInvolvements.createAllFor(behaviorVersion, users, OffsetDateTime.now)
      } yield {}
    }.getOrElse(Future.successful({}))
  }

  private def postChatMessage(
                               maybeConversationOverride: Option[ConversationAccount],
                               maybeReplyToId: Option[String],
                               text: String,
                               maybeAttachments: Option[Seq[Attachment]] = None
                             )(implicit actorSystem: ActorSystem, ec: ExecutionContext): Future[String] = {
    for {
      aadUserMembers <- client.getAllUsers
      members <- Future.sequence(aadUserMembers.map { ea =>
        client.maybeEllipsisTeamId.map { teamId =>
          MSTeamsUser.maybeForMSAADUser(ea, teamId, services.dataService)
        }.getOrElse(Future.successful(None))
      }).map(_.flatten)
      result <- info match {
        case activityInfo: ActivityInfo => {
          val conversation = maybeConversationOverride.getOrElse(activityInfo.conversation)
          val response = ResponseInfo.newForMessage(
            info.botParticipant,
            conversation,
            activityInfo.maybeUserParticipant,
            text,
            "markdown",
            maybeReplyToId,
            maybeAttachments,
            members
          )
          client.postToResponseUrl(
            activityInfo.responseUrlFor(conversation.id, maybeReplyToId),
            Json.toJson(response)
          ).
            recover(postErrorRecovery(activityInfo.conversation.id, text)).
            flatMap { res =>
              logInvolvedFor(activityInfo).map(_ => res)
            }
        }
        case firstMessageInfo: FirstMessageInfo => {
          val convoId = firstMessageInfo.channel
          for {
            result <- {
              val response = ResponseInfo.newForMessage(
                firstMessageInfo.botParticipant,
                ConversationAccount(convoId, Some(firstMessageInfo.isPublicChannel), None, firstMessageInfo.conversationType),
                firstMessageInfo.maybeUserParticipant,
                text,
                "markdown",
                maybeReplyToId,
                maybeAttachments,
                members
              )
              client.postToResponseUrl(
                s"${firstMessageInfo.serviceUrl}/v3/conversations/$convoId/activities",
                Json.toJson(response)
              ).
                recover(postErrorRecovery(convoId, text)).
                flatMap { res =>
                  logInvolvedFor(firstMessageInfo).map(_ => res)
                }
            }
          } yield result
        }
      }
    } yield result
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

  private def isMovingToPrivate: Boolean = responseType == Private && info.conversationType != "personal"

  private def maybePreambleText: Option[String] = {
    if (isMovingToPrivate) {
      info.maybeUserParticipant.map { from =>
        s"<at>${from.name}</at> I’ve sent you a [private message](${client.botDMDeepLink}) :sleuth_or_spy:"
      }
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
          postChatMessage(None, info.maybeId, preambleMessage, None)
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

        if (isMovingToPrivate) {
          getPrivateConversationId.flatMap { convoId =>
            postChatMessage(Some(ConversationAccount(convoId, None, None, "personal")), None, segment, maybeAttachmentsForSegment)
          }
        } else {
          postChatMessage(None, info.maybeId, segment, maybeAttachmentsForSegment)
        }
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

  def userIdsInText(text: String): Set[String] = {
    """<@(.+?)>""".r.findAllMatchIn(text).flatMap(_.subgroups).toSet
  }

  def userDataListFor(text: String, team: EllipsisTeam)(implicit ec: ExecutionContext): Future[Set[UserData]] = {
    val ids = userIdsInText(text)
    Future.sequence(ids.map { eaId =>
      for {
        user <- services.dataService.users.ensureUserFor(LoginInfo(Conversation.MS_TEAMS_CONTEXT, eaId), Seq(), team.id)
        userData <- services.dataService.users.userDataFor(user, team)
      } yield userData.copy(userIdForContext = Some(eaId))
    })
  }

  def send(implicit actorSystem: ActorSystem, ec: ExecutionContext): Future[Option[MSTeamsMessage]] = {
    val attachments = attachmentsToUse.flatMap {
      case a: MSTeamsMessageAttachment => Some(a.underlying)
      case _ => None
    }
    for {
      maybeTeam <- client.maybeEllipsisTeamId.map { tid =>
        services.dataService.teams.find(tid)
      }.getOrElse(Future.successful(None))
      userDataList <- maybeTeam.map { team =>
        userDataListFor(unformattedText, team)
      }.getOrElse(Future.successful(Set[UserData]()))
      formattedText <- Future.successful(MSTeamsMessageFormatter.bodyTextFor(unformattedText, userDataList))
      _ <- sendPreamble(formattedText)
      maybeLastTs <- sendMessageSegmentsInOrder(messageSegmentsFor(formattedText), originatingChannel, maybeShouldUnfurl, attachments, maybeConversation, None)
      _ <- sendFiles
    } yield maybeLastTs.map(MSTeamsMessage.apply)
  }

  private def postErrorRecovery[U](convoId: String, text: String): PartialFunction[Throwable, U] = {
    case t: Throwable => throw MSTeamsMessageSenderException(t, convoId, teamIdForContext, user, text)
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

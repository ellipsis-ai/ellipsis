package models.behaviors.events

import akka.actor.ActorSystem
import json.Formatting._
import json.SlackUserData
import models.accounts.ms_teams.botprofile.MSTeamsBotProfile
import models.accounts.slack.botprofile.SlackBotProfile
import models.accounts.user.User
import models.behaviors.behaviorversion.{BehaviorResponseType, Private}
import models.behaviors.conversations.conversation.Conversation
import models.behaviors.{ActionChoice, BotInfo, BotResult, DeveloperContext}
import models.team.Team
import play.api.Logger
import play.api.libs.json.{JsObject, JsValue, Json}
import services.DefaultServices
import services.ms_teams.apiModels.{ActivityInfo, ResponseInfo}
import services.slack.SlackApiError
import slick.dbio.DBIO
import utils.{SlackChannels, SlackMessageReactionHandler, SlackMessageSender, UploadFileSpec}

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.{ExecutionContext, Future}

sealed trait EventContext {

  val isPublicChannel: Boolean
  val isDirectMessage: Boolean
  val maybeChannel: Option[String]
  val name: String
  val userId: String
  val teamId: String

  def eventualMaybeDMChannel(services: DefaultServices)(implicit actorSystem: ActorSystem, ec: ExecutionContext): Future[Option[String]]

  def maybeChannelForSendAction(
                                 responseType: BehaviorResponseType,
                                 maybeConversation: Option[Conversation],
                                 services: DefaultServices
                               )(implicit ec: ExecutionContext, actorSystem: ActorSystem): DBIO[Option[String]]

  def botName(services: DefaultServices)(implicit actorSystem: ActorSystem, ec: ExecutionContext): Future[String]

  def messageRecipientPrefix(isUninterruptedConversation: Boolean): String

  def detailsFor(services: DefaultServices)(implicit actorSystem: ActorSystem, ec: ExecutionContext): Future[JsObject]

  def maybeThreadId: Option[String]
  def maybeTeamIdForContext: Option[String]
  def maybeUserIdForContext: Option[String]
  def maybeBotUserIdForContext: Option[String]
  def maybeBotInfo(services: DefaultServices)(implicit actorSystem: ActorSystem, ec: ExecutionContext): Future[Option[BotInfo]]

  def sendMessage(
                   event: Event,
                   text: String,
                   responseType: BehaviorResponseType,
                   maybeShouldUnfurl: Option[Boolean],
                   maybeConversation: Option[Conversation],
                   attachmentGroups: Seq[MessageAttachmentGroup],
                   files: Seq[UploadFileSpec],
                   choices: Seq[ActionChoice],
                   developerContext: DeveloperContext,
                   services: DefaultServices
                 )(implicit actorSystem: ActorSystem, ec: ExecutionContext): Future[Option[String]]

}

case class SlackEventContext(
                              profile: SlackBotProfile,
                              channel: String,
                              maybeThreadId: Option[String],
                              userId: String
                            ) extends EventContext {

  def maybeBotInfo(services: DefaultServices)(implicit actorSystem: ActorSystem, ec: ExecutionContext): Future[Option[BotInfo]] = {
    botName(services).map { botName =>
      Some(BotInfo(botName, profile.userId))
    }
  }

  val teamId: String = profile.teamId
  lazy val name: String = Conversation.SLACK_CONTEXT
  val maybeChannel: Option[String] = Some(channel)
  val maybeTeamIdForContext: Option[String] = Some(profile.slackTeamId)
  def maybeUserIdForContext: Option[String] = Some(userId)
  def maybeBotUserIdForContext: Option[String] = Some(profile.userId)

  def eventualMaybeDMChannel(services: DefaultServices)(implicit actorSystem: ActorSystem, ec: ExecutionContext): Future[Option[String]] = {
    if (profile.userId == userId) {
      Future.successful(None)
    } else {
      services.slackApiService.clientFor(profile).openConversationFor(userId).map(Some(_)).recover {
        case e: SlackApiError => {
          if (e.code != "cannot_dm_bot") {
            val msg =
              s"""Couldn't open DM channel to user with ID ${userId} on Slack team ${profile.slackTeamId} due to Slack API error: ${e.code}
                 |Original event channel: $channel
               """.stripMargin
            Logger.error(msg, e)
          }
          None
        }
      }
    }
  }

  def channelForSend(
                      responseType: BehaviorResponseType,
                      maybeConversation: Option[Conversation],
                      services: DefaultServices
                    )(implicit actorSystem: ActorSystem, ec: ExecutionContext): Future[String] = {
    (if (responseType == Private) {
      eventualMaybeDMChannel(services)
    } else {
      Future.successful(maybeConversation.flatMap(_.maybeChannel))
    }).map { maybeChannel =>
      maybeChannel.getOrElse(channel)
    }
  }

  def maybeChannelForSendAction(
                                 responseType: BehaviorResponseType,
                                 maybeConversation: Option[Conversation],
                                 services: DefaultServices
                               )(implicit ec: ExecutionContext, actorSystem: ActorSystem): DBIO[Option[String]] = {
    DBIO.from(channelForSend(responseType, maybeConversation, services).map(Some(_)))
  }

  val isDirectMessage: Boolean = {
    channel.startsWith("D")
  }
  val isPrivateChannel: Boolean = {
    channel.startsWith("G")
  }
  val isPublicChannel: Boolean = {
    !isDirectMessage && !isPrivateChannel
  }

  def messageRecipientPrefix(isUninterruptedConversation: Boolean): String = {
    if (isDirectMessage || isUninterruptedConversation) {
      ""
    } else {
      s"<@$userId>: "
    }
  }

  def botName(services: DefaultServices)(implicit actorSystem: ActorSystem, ec: ExecutionContext): Future[String] = {
    services.dataService.slackBotProfiles.maybeNameFor(profile).map { maybeName =>
      maybeName.getOrElse(SlackMessageEvent.fallbackBotPrefix)
    }
  }

  private def profileDataFor(slackUserData: SlackUserData): JsObject = {
    val profile: JsValue = slackUserData.profile.map(Json.toJson(_)).getOrElse(JsObject.empty)
    Json.obj(
      "name" -> slackUserData.getDisplayName,
      "profile" -> profile,
      "isPrimaryOwner" -> slackUserData.isPrimaryOwner,
      "isOwner" -> slackUserData.isOwner,
      "isRestricted" -> slackUserData.isRestricted,
      "isUltraRestricted" -> slackUserData.isUltraRestricted,
      "tz" -> slackUserData.tz
    )
  }

  def detailsFor(services: DefaultServices)(implicit actorSystem: ActorSystem, ec: ExecutionContext): Future[JsObject] = {
    val client = services.slackApiService.clientFor(profile)
    val slackChannels = SlackChannels(client)
    for {
      maybeUser <- services.slackEventService.maybeSlackUserDataFor(userId, client, (e) => {
        Logger.error(
          s"""Slack API reported user not found while generating details about the user to send to an action:
             |Slack user ID: ${userId}
             |Ellipsis bot Slack team ID: ${profile.slackTeamId}
             |Ellipsis team ID: ${profile.teamId}
           """.stripMargin, e)
        None
      })
      maybeChannelInfo <- slackChannels.getInfoFor(channel)
      members <- slackChannels.getMembersFor(channel)
    } yield {
      val channelDetails = JsObject(Seq(
        "channelMembers" -> Json.toJson(members),
        "channelName" -> Json.toJson(maybeChannelInfo.map(_.computedName))
      ))
      maybeUser.map { user =>
        profileDataFor(user) ++ channelDetails
      }.getOrElse {
        channelDetails
      }
    }
  }

  def sendMessage(
                   event: Event,
                   unformattedText: String,
                   responseType: BehaviorResponseType,
                   maybeShouldUnfurl: Option[Boolean],
                   maybeConversation: Option[Conversation],
                   attachmentGroups: Seq[MessageAttachmentGroup],
                   files: Seq[UploadFileSpec],
                   choices: Seq[ActionChoice],
                   developerContext: DeveloperContext,
                   services: DefaultServices
                 )(implicit actorSystem: ActorSystem, ec: ExecutionContext): Future[Option[String]] = {
    for {
      maybeDMChannel <- eventualMaybeDMChannel(services)
      botName <- botName(services)
      maybeTs <- SlackMessageSender(
        services.slackApiService.clientFor(profile),
        userId,
        profile.slackTeamId,
        unformattedText,
        responseType,
        developerContext,
        channel,
        maybeDMChannel,
        maybeThreadId,
        maybeShouldUnfurl,
        maybeConversation,
        attachmentGroups,
        files,
        choices,
        services.configuration,
        botName,
        event.messageUserDataList(maybeConversation, services),
        services,
        event.isEphemeral,
        event.maybeResponseUrl,
        event.beQuiet
      ).send
    } yield maybeTs
  }

  def maybePermalinkFor(
                          messageTs: String,
                          services: DefaultServices
                        )(implicit actorSystem: ActorSystem, ec: ExecutionContext): Future[Option[String]] = {
    val client = services.slackApiService.clientFor(profile)
    client.permalinkFor(channel, messageTs)
  }

  def reactionHandler(eventualResults: Future[Seq[BotResult]], maybeMessageTs: Option[String], services: DefaultServices)
                     (implicit ec: ExecutionContext, actorSystem: ActorSystem): Future[Seq[BotResult]] = {
    maybeMessageTs.map { messageTs =>
      SlackMessageReactionHandler.handle(services.slackApiService.clientFor(profile), eventualResults, channel, messageTs)
    }
    eventualResults
  }

}

case class MSTeamsEventContext(
                              profile: MSTeamsBotProfile,
                              info: ActivityInfo
                              ) extends EventContext {

  import services.ms_teams.apiModels.Formatting._

  val name: String = Conversation.MS_TEAMS_CONTEXT
  val userId: String = info.from.id
  val teamId: String = profile.teamId
  val botUserIdForContext: String = info.recipient.id

  val isDirectMessage: Boolean = {
    info.conversation.conversationType == "personal"
  }
  val isPublicChannel: Boolean = {
    info.conversation.conversationType == "team"
  }
  val channel: String = info.conversation.id
  val maybeChannel: Option[String] = Some(channel)

  def maybeTeamIdForContext: Option[String] = Some(profile.teamIdForContext)

  def maybeBotUserIdForContext: Option[String] = Some(botUserIdForContext)

  def maybeUserIdForContext: Option[String] = Some(userId)

  def maybeThreadId: Option[String] = Some(info.conversation.id)

  def eventualMaybeDMChannel(services: DefaultServices)(implicit actorSystem: ActorSystem, ec: ExecutionContext): Future[Option[String]] = {
    Future.successful(None)
  }

  def messageRecipientPrefix(isUninterruptedConversation: Boolean): String = ""

  def botName(services: DefaultServices)(implicit actorSystem: ActorSystem, ec: ExecutionContext): Future[String] = {
    Future.successful("Ellipsis")
  }

  def maybeBotInfo(services: DefaultServices)(implicit actorSystem: ActorSystem, ec: ExecutionContext): Future[Option[BotInfo]] = {
    botName(services).map { botName =>
      Some(BotInfo(botName, botUserIdForContext))
    }
  }

  def maybeChannelForSendAction(
                                 responseType: BehaviorResponseType,
                                 maybeConversation: Option[Conversation],
                                 services: DefaultServices
                               )(implicit ec: ExecutionContext, actorSystem: ActorSystem): DBIO[Option[String]] = {
    DBIO.successful(None)
  }

  val maybeResponseUrl: Option[String] = Some(info.responseUrl)

  def sendMessage(
                   event: Event,
                   unformattedText: String,
                   responseType: BehaviorResponseType,
                   maybeShouldUnfurl: Option[Boolean],
                   maybeConversation: Option[Conversation],
                   attachmentGroups: Seq[MessageAttachmentGroup],
                   files: Seq[UploadFileSpec],
                   choices: Seq[ActionChoice],
                   developerContext: DeveloperContext,
                   services: DefaultServices
                 )(implicit actorSystem: ActorSystem, ec: ExecutionContext): Future[Option[String]] = {
    val response = ResponseInfo(
      "message",
      info.recipient,
      info.conversation,
      info.from,
      unformattedText,
      info.id
    )
    val apiClient = services.msTeamsApiService.profileClientFor(profile)
    for {
      _ <- maybeResponseUrl.map { responseUrl =>
        apiClient.postToResponseUrl(responseUrl, Json.toJson(response)).map(_ => {})
      }.getOrElse(Future.successful({}))
    } yield None // TODO: might be something to return here
  }

  def detailsFor(services: DefaultServices)(implicit actorSystem: ActorSystem, ec: ExecutionContext): Future[JsObject] = {
    Future.successful(JsObject(Seq())) // TODO: this
  }

}

case class TestEventContext(
                             user: User,
                             team: Team
                           ) extends EventContext {

  val name = "test"
  val isPublicChannel: Boolean = false
  val isDirectMessage: Boolean = false
  val maybeChannel: Option[String] = None
  val userId: String = user.id
  val teamId: String = team.id
  val maybeTeamIdForContext: Option[String] = None
  val maybeThreadId: Option[String] = None
  def maybeBotInfo(services: DefaultServices)(implicit actorSystem: ActorSystem, ec: ExecutionContext): Future[Option[BotInfo]] = {
    Future.successful(None)
  }
  def maybeUserIdForContext: Option[String] = None
  def maybeBotUserIdForContext: Option[String] = None
  def eventualMaybeDMChannel(services: DefaultServices)(implicit actorSystem: ActorSystem, ec: ExecutionContext) = Future.successful(None)

  def maybeChannelForSendAction(
                                 responseType: BehaviorResponseType,
                                 maybeConversation: Option[Conversation],
                                 services: DefaultServices
                               )(implicit ec: ExecutionContext, actorSystem: ActorSystem): DBIO[Option[String]] = DBIO.successful(None)

  def botName(services: DefaultServices)(implicit actorSystem: ActorSystem, ec: ExecutionContext): Future[String] = {
    Future.successful(s"${team.name} TestBot")
  }

  def messageRecipientPrefix(isUninterruptedConversation: Boolean): String = ""

  def detailsFor(services: DefaultServices)(implicit actorSystem: ActorSystem, ec: ExecutionContext): Future[JsObject] = {
    Future.successful(JsObject(Seq()))
  }

  val messageBuffer: ArrayBuffer[String] = new ArrayBuffer()

  def sendMessage(
                   event: Event,
                   text: String,
                   responseType: BehaviorResponseType,
                   maybeShouldUnfurl: Option[Boolean],
                   maybeConversation: Option[Conversation],
                   attachmentGroups: Seq[MessageAttachmentGroup],
                   files: Seq[UploadFileSpec],
                   choices: Seq[ActionChoice],
                   developerContext: DeveloperContext,
                   services: DefaultServices
                 )(implicit actorSystem: ActorSystem, ec: ExecutionContext): Future[Option[String]] = {
    Future.successful(messageBuffer += text).map(_ => None)
  }
}

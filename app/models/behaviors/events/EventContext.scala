package models.behaviors.events

import akka.actor.ActorSystem
import com.mohiva.play.silhouette.api.LoginInfo
import json.Formatting._
import json.SlackUserData
import models.accounts.slack.botprofile.SlackBotProfile
import models.accounts.user.User
import models.behaviors.behaviorversion.{BehaviorResponseType, Private}
import models.behaviors.conversations.conversation.Conversation
import models.behaviors.{ActionChoice, BotInfo, BotResult, DeveloperContext}
import models.team.Team
import play.api.Logger
import play.api.libs.json.{JsObject, JsValue, Json}
import services.{DataService, DefaultServices}
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

  val ellipsisTeamId: String
  val userIdForContext: String
  val teamIdForContext: String

  def maybeBotInfo(services: DefaultServices)(implicit actorSystem: ActorSystem, ec: ExecutionContext): Future[Option[BotInfo]]
  def eventualMaybeDMChannel(services: DefaultServices)(implicit actorSystem: ActorSystem, ec: ExecutionContext): Future[Option[String]]

  def ensureUserAction(dataService: DataService): DBIO[User]

  def maybeChannelForSendAction(
                                 responseType: BehaviorResponseType,
                                 maybeConversation: Option[Conversation],
                                 services: DefaultServices
                               )(implicit ec: ExecutionContext, actorSystem: ActorSystem): DBIO[Option[String]]

  def botName(services: DefaultServices)(implicit actorSystem: ActorSystem, ec: ExecutionContext): Future[String]

  def messageRecipientPrefix(isUninterruptedConversation: Boolean): String

  def detailsFor(services: DefaultServices)(implicit actorSystem: ActorSystem, ec: ExecutionContext): Future[JsObject]

  def maybeThreadId: Option[String]

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
                              userIdForContext: String
                            ) extends EventContext {

  def maybeBotInfo(services: DefaultServices)(implicit actorSystem: ActorSystem, ec: ExecutionContext): Future[Option[BotInfo]] = {
    botName(services).map { botName =>
      Some(BotInfo(botName, profile.userId))
    }
  }

  def loginInfo: LoginInfo = LoginInfo(name, userIdForContext)
  def ensureUserAction(dataService: DataService): DBIO[User] = {
    dataService.users.ensureUserForAction(loginInfo, ellipsisTeamId)
  }

  val ellipsisTeamId: String = profile.teamId
  lazy val name: String = Conversation.SLACK_CONTEXT
  val maybeChannel: Option[String] = Some(channel)
  override val teamIdForContext: String = profile.slackTeamId
  val botUserId: String = profile.userId
  val isBotMessage: Boolean = botUserId == userIdForContext

  def eventualMaybeDMChannel(services: DefaultServices)(implicit actorSystem: ActorSystem, ec: ExecutionContext): Future[Option[String]] = {
    if (isBotMessage) {
      Future.successful(None)
    } else {
      services.slackApiService.clientFor(profile).openConversationFor(userIdForContext).map(Some(_)).recover {
        case e: SlackApiError => {
          if (e.code != "cannot_dm_bot") {
            val msg =
              s"""Couldn't open DM channel to user with ID ${userIdForContext} on Slack team ${profile.slackTeamId} due to Slack API error: ${e.code}
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
      s"<@$userIdForContext>: "
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
      maybeUser <- services.slackEventService.maybeSlackUserDataFor(userIdForContext, client, (e) => {
        Logger.error(
          s"""Slack API reported user not found while generating details about the user to send to an action:
             |Slack user ID: ${userIdForContext}
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
        userIdForContext,
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

case class TestEventContext(
                             user: User,
                             team: Team
                           ) extends EventContext {

  val name = "test"
  val isPublicChannel: Boolean = false
  val isDirectMessage: Boolean = false
  val maybeChannel: Option[String] = None
  val maybeThreadId: Option[String] = None

  val userIdForContext: String = user.id

  // these are the same for the test context:
  val ellipsisTeamId: String = team.id
  val teamIdForContext: String = ellipsisTeamId

  def maybeBotInfo(services: DefaultServices)(implicit actorSystem: ActorSystem, ec: ExecutionContext): Future[Option[BotInfo]] = {
    Future.successful(None)
  }
  val isBotMessage: Boolean = false

  def ensureUserAction(dataService: DataService): DBIO[User] = DBIO.successful(user)
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

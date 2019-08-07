package models.behaviors.events

import akka.actor.ActorSystem
import com.mohiva.play.silhouette.api.LoginInfo
import json.Formatting._
import json.{DialogState, SlackDialogInput, SlackUserData, UserData}
import models.accounts.{MSTeamsContext, SlackContext}
import models.accounts.ms_teams.botprofile.MSTeamsBotProfile
import models.accounts.slack.botprofile.SlackBotProfile
import models.accounts.user.User
import models.behaviors._
import models.behaviors.behaviorversion.{BehaviorResponseType, BehaviorVersion, Private}
import models.behaviors.conversations.conversation.Conversation
import models.behaviors.dialogs.Dialog
import models.behaviors.ellipsisobject.{BotInfo, Channel}
import models.behaviors.events.ms_teams._
import models.behaviors.events.slack._
import models.behaviors.testing.TestRunEvent
import models.team.Team
import play.api.Logger
import play.api.libs.json.{JsObject, JsString, JsValue, Json}
import services.caching.SlackMessagePermalinkCacheKey
import services.ms_teams.apiModels.{ActivityInfo, EventInfo}
import services.slack.SlackApiError
import services.{DataService, DefaultServices}
import slick.dbio.DBIO
import utils._

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.{ExecutionContext, Future}

sealed trait EventContext {

  type MessageAttachmentType <: MessageAttachment
  type MessageActionType <: MessageAction
  type MenuType <: MessageMenu
  type MenuItemType <: MessageMenuItem
  type MessageEventType <: MessageEvent

  val isPublicChannel: Boolean
  val isDirectMessage: Boolean
  val maybeChannel: Option[String]
  val name: String
  val description: String

  val ellipsisTeamId: String
  val userIdForContext: String
  val teamIdForContext: String

  def shouldForceRequireMention: Boolean = false

  def maybeBotInfo(services: DefaultServices)(implicit actorSystem: ActorSystem, ec: ExecutionContext): Future[Option[BotInfo]]
  def eventualMaybeDMChannel(services: DefaultServices)(implicit actorSystem: ActorSystem, ec: ExecutionContext): Future[Option[String]]

  def loginInfo: LoginInfo
  def otherLoginInfos: Seq[LoginInfo]
  def ensureUserAction(dataService: DataService): DBIO[User] = {
    dataService.users.ensureUserForAction(loginInfo, otherLoginInfos, ellipsisTeamId)
  }

  def maybeChannelForSendAction(
                                 responseType: BehaviorResponseType,
                                 maybeConversation: Option[Conversation],
                                 services: DefaultServices
                               )(implicit ec: ExecutionContext, actorSystem: ActorSystem): DBIO[Option[String]]

  def botName(services: DefaultServices)(implicit actorSystem: ActorSystem, ec: ExecutionContext): Future[String]

  def messageRecipientPrefix(isUninterruptedConversation: Boolean): String

  def channelDetailsFor(services: DefaultServices)(implicit actorSystem: ActorSystem, ec: ExecutionContext): Future[JsObject]
  def maybeUserDetailsFor(services: DefaultServices)(implicit actorSystem: ActorSystem, ec: ExecutionContext): Future[Option[JsObject]]

  def detailsFor(services: DefaultServices)(implicit actorSystem: ActorSystem, ec: ExecutionContext): Future[JsObject] = {
    for {
      channelDetails <- channelDetailsFor(services)
      maybeUserDetails <- maybeUserDetailsFor(services)
    } yield {
      Seq(Some(channelDetails), maybeUserDetails).flatten.reduce(_ ++ _)
    }
  }

  def maybeChannelDataForAction(services: DefaultServices)(implicit ec: ExecutionContext): DBIO[Option[Channel]]

  def maybeThreadId: Option[String]

  def sendMessage(
                   event: Event,
                   text: String,
                   maybeBehaviorVersion: Option[BehaviorVersion],
                   responseType: BehaviorResponseType,
                   maybeShouldUnfurl: Option[Boolean],
                   maybeConversation: Option[Conversation],
                   attachments: Seq[MessageAttachment],
                   files: Seq[UploadFileSpec],
                   choices: Seq[ActionChoice],
                   developerContext: DeveloperContext,
                   services: DefaultServices
                 )(implicit actorSystem: ActorSystem, ec: ExecutionContext): Future[Option[Message]]

  def maybeOpenDialog(
                  event: Event,
                  dialog: Dialog,
                  developerContext: DeveloperContext,
                  services: DefaultServices
                )(implicit actorSystem: ActorSystem, ec: ExecutionContext): Future[Option[Boolean]]

  def stateForDialog(event: Event, parametersWithValues: Seq[ParameterWithValue]): DialogState = {
    val arguments = parametersWithValues.flatMap { p =>
      p.maybeValue.map { value =>
        (p.parameter.input.name, value.text)
      }
    }.toMap
    DialogState(event.maybeMessageId, maybeThreadId, arguments)
  }

  def newRunEventFor(
                   botResult: BotResult,
                   nextAction: NextAction,
                   behaviorVersion: BehaviorVersion,
                   channel: String,
                   maybeMessageId: Option[String],
                   maybeThreadId: Option[String]
                 ): Event

  def reactionHandler(eventualResults: Future[Seq[BotResult]], maybeMessageTs: Option[String], services: DefaultServices)
                     (implicit ec: ExecutionContext, actorSystem: ActorSystem): Future[Seq[BotResult]]

  def messageActionButtonFor(
                              callbackId: String,
                              label: String,
                              value: String,
                              maybeStyle: Option[String] = None
                            ): MessageActionType

  def messageActionMenuItemFor(
                                text: String,
                                value: String
                              ): MenuItemType

  def messageActionMenuFor(
                            name: String,
                            text: String,
                            options: Seq[MenuItemType]
                          ): MessageActionType

  def maybeMessageActionTextInputFor(
                                      name: String
                                    ): Option[MessageActionType]

  def messageAttachmentFor(
                            maybeText: Option[String] = None,
                            maybeUserDataList: Option[Set[UserData]] = None,
                            maybeTitle: Option[String] = None,
                            maybeTitleLink: Option[String] = None,
                            maybeColor: Option[Color] = None,
                            maybeCallbackId: Option[String] = None,
                            actions: Seq[MessageActionType] = Seq()
                          ): MessageAttachmentType

}

case class SlackEventContext(
                              profile: SlackBotProfile,
                              channel: String,
                              maybeThreadId: Option[String],
                              userIdForContext: String
                            ) extends EventContext {

  override type MessageAttachmentType = SlackMessageAttachment
  override type MessageActionType = SlackMessageAction
  override type MenuType = SlackMessageMenu
  override type MenuItemType = SlackMessageMenuItem
  override type MessageEventType = SlackMessageEvent

  def maybeBotInfo(services: DefaultServices)(implicit actorSystem: ActorSystem, ec: ExecutionContext): Future[Option[BotInfo]] = {
    botName(services).map { botName =>
      Some(BotInfo(botName, profile.userId))
    }
  }

  val ellipsisTeamId: String = profile.teamId
  val name: String = SlackContext.name
  val description: String = SlackContext.description
  val maybeChannel: Option[String] = Some(channel)
  val teamIdForContext: String = profile.slackTeamId
  val botUserId: String = profile.userId
  val isBotMessage: Boolean = botUserId == userIdForContext

  def loginInfo: LoginInfo = LoginInfo(name, userIdForContext)
  def otherLoginInfos: Seq[LoginInfo] = Seq()

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
    SlackEventContext.channelIsDM(channel)
  }
  val isPrivateChannel: Boolean = {
    SlackEventContext.channelIsPrivateChannel(channel)
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

  def channelDetailsFor(services: DefaultServices)(implicit actorSystem: ActorSystem, ec: ExecutionContext): Future[JsObject] = {
    val client = services.slackApiService.clientFor(profile)
    val slackChannels = SlackChannels(client)
    for {
      maybeChannelInfo <- slackChannels.getInfoFor(channel)
      members <- slackChannels.getMembersFor(channel)
    } yield JsObject(Seq(
      "channelMembers" -> Json.toJson(members),
      "channelName" -> Json.toJson(maybeChannelInfo.map(_.computedName))
    ))
  }

  def maybeChannelDataForAction(services: DefaultServices)(implicit ec: ExecutionContext): DBIO[Option[Channel]] = {
    Channel.buildForSlackAction(channel, profile, services).map(Some(_))
  }

  def maybeUserDetailsFor(services: DefaultServices)(implicit actorSystem: ActorSystem, ec: ExecutionContext): Future[Option[JsObject]] = {
    val client = services.slackApiService.clientFor(profile)
    services.slackEventService.maybeSlackUserDataFor(userIdForContext, client, (e) => {
      Logger.error(
        s"""Slack API reported user not found while generating details about the user to send to an action:
           |Slack user ID: ${userIdForContext}
           |Ellipsis bot Slack team ID: ${profile.slackTeamId}
           |Ellipsis team ID: ${profile.teamId}
           """.stripMargin, e)
      None
    }).map { maybeUser =>
      maybeUser.map { user =>
        profileDataFor(user)
      }
    }
  }

  def sendMessage(
                   event: Event,
                   unformattedText: String,
                   maybeBehaviorVersion: Option[BehaviorVersion],
                   responseType: BehaviorResponseType,
                   maybeShouldUnfurl: Option[Boolean],
                   maybeConversation: Option[Conversation],
                   attachments: Seq[MessageAttachment],
                   files: Seq[UploadFileSpec],
                   choices: Seq[ActionChoice],
                   developerContext: DeveloperContext,
                   services: DefaultServices
                 )(implicit actorSystem: ActorSystem, ec: ExecutionContext): Future[Option[Message]] = {
    for {
      maybeDMChannel <- eventualMaybeDMChannel(services)
      botName <- botName(services)
      userDataList <- event.messageUserDataList(maybeConversation, services)
      maybeMessage <- SlackMessageSender(
        services.slackApiService.clientFor(profile),
        userIdForContext,
        profile.slackTeamId,
        unformattedText,
        responseType,
        developerContext,
        originatingChannel = channel,
        maybeDMChannel,
        event.maybeMessageId,
        maybeThreadId,
        maybeShouldUnfurl,
        maybeConversation,
        attachments,
        files,
        choices,
        services.configuration,
        botName,
        userDataList,
        services,
        event.isEphemeral,
        event.maybeResponseUrl,
        event.beQuiet,
        maybeBehaviorVersion
      ).send
    } yield maybeMessage
  }

  private def maybePermalinkFunctionFor(key: SlackMessagePermalinkCacheKey, services: DefaultServices): SlackMessagePermalinkCacheKey => Future[Option[String]] = {
    (key) => {
      val client = services.slackApiService.clientFor(profile)
      client.permalinkFor(key.channel, key.messageTs)
    }
  }

  def maybePermalinkFor(
                          messageTs: String,
                          services: DefaultServices
                        )(implicit actorSystem: ActorSystem, ec: ExecutionContext): Future[Option[String]] = {
    val key = SlackMessagePermalinkCacheKey(messageTs, channel, profile.slackTeamId)
    services.cacheService.getSlackPermalinkForMessage(key, maybePermalinkFunctionFor(key, services))
  }

  val DIALOG_INPUT_MAX = 10

  def maybeOpenDialog(
                       event: Event,
                       dialog: Dialog,
                       developerContext: DeveloperContext,
                       services: DefaultServices
                     )(implicit actorSystem: ActorSystem, ec: ExecutionContext): Future[Option[Boolean]] = {
    val client = services.slackApiService.clientFor(profile)
    val inputs: Seq[SlackDialogInput] = dialog.parametersWithValues.
      filter(_.maybeValue.isEmpty).
      flatMap(ea => SlackDialogInput.maybeFromInput(ea.parameter.input))
    if (inputs.length > 1) {
      client.openDialog(dialog, inputs.slice(0, DIALOG_INPUT_MAX)).map(Some(_))
    } else {
      Future.successful(None)
    }
  }

  def reactionHandler(eventualResults: Future[Seq[BotResult]], maybeMessageTs: Option[String], services: DefaultServices)
                     (implicit ec: ExecutionContext, actorSystem: ActorSystem): Future[Seq[BotResult]] = {
    maybeMessageTs.map { messageTs =>
      SlackMessageReactionHandler.handle(services.slackApiService.clientFor(profile), eventualResults, channel, messageTs)
    }
    eventualResults
  }

  def newRunEventFor(
                    botResult: BotResult,
                    nextAction: NextAction,
                    behaviorVersion: BehaviorVersion,
                    channel: String,
                    maybeMessageId: Option[String],
                    maybeThreadId: Option[String]
                  ): Event = {
    val eventContext = SlackEventContext(
      profile,
      channel,
      maybeThreadId,
      userIdForContext
    )
    SlackRunEvent(
      eventContext,
      behaviorVersion,
      nextAction.argumentsMap,
      EventType.nextAction,
      Some(botResult.event.originalEventType),
      maybeScheduled = None,
      botResult.event.isEphemeral,
      botResult.event.maybeResponseUrl,
      maybeMessageId
    )
  }

  def messageActionButtonFor(
                              callbackId: String,
                              label: String,
                              value: String,
                              maybeStyle: Option[String] = None
                            ) = {
    SlackMessageActionButton(callbackId, label, value)
  }

  def messageActionMenuItemFor(
                                text: String,
                                value: String
                              ): SlackMessageMenuItem = {
    SlackMessageMenuItem(text, value)
  }

  def messageActionMenuFor(
                            name: String,
                            text: String,
                            options: Seq[SlackMessageMenuItem]
                          ): SlackMessageMenu = {
    SlackMessageMenu(name, text, options)
  }

  def maybeMessageActionTextInputFor(
                                      name: String
                                    ): Option[MessageActionType] = None

  def messageAttachmentFor(
                            maybeText: Option[String] = None,
                            maybeUserDataList: Option[Set[UserData]] = None,
                            maybeTitle: Option[String] = None,
                            maybeTitleLink: Option[String] = None,
                            maybeColor: Option[Color] = None,
                            maybeCallbackId: Option[String] = None,
                            actions: Seq[SlackMessageAction] = Seq()
                          ): SlackMessageAttachment = {
    SlackMessageAttachment(maybeText, maybeUserDataList, maybeTitle, maybeTitleLink, maybeColor, maybeCallbackId, actions)
  }

}

object SlackEventContext {
  def channelIsDM(channel: String): Boolean = {
    channel.startsWith("D")
  }

  def channelIsPrivateChannel(channel: String): Boolean = {
    channel.startsWith("G")
  }
}

case class MSTeamsEventContext(
                              profile: MSTeamsBotProfile,
                              info: EventInfo
                              ) extends EventContext {

  override type MessageAttachmentType = MSTeamsMessageAttachment
  override type MessageActionType = MSTeamsMessageAction
  override type MenuType = MSTeamsMessageMenu
  override type MenuItemType = MSTeamsMessageMenuItem
  override type MessageEventType = MSTeamsMessageEvent

  val name: String = MSTeamsContext.name
  val description: String = MSTeamsContext.description
  val userIdForContext: String = info.userIdForContext
  val teamIdForContext: String = profile.teamIdForContext
  val ellipsisTeamId: String = profile.teamId
  val botUserIdForContext: String = info.botUserIdForContext

  def loginInfo: LoginInfo = LoginInfo(Conversation.MS_TEAMS_CONTEXT, info.userIdForContext)
  def otherLoginInfos: Seq[LoginInfo] = info.aadObjectId.map(id => LoginInfo(Conversation.MS_AAD_CONTEXT, id)).toSeq

  val isDirectMessage: Boolean = info.isDirectMessage
  val isPublicChannel: Boolean = info.isPublicChannel
  val channel: String = info.channel
  val maybeChannel: Option[String] = Some(channel)

  val fallbackBotPrefix: String = "EllipsisAi"

  override def shouldForceRequireMention: Boolean = isPublicChannel

  def maybeBotUserIdForContext: Option[String] = Some(botUserIdForContext)

  def maybeUserIdForContext: Option[String] = Some(userIdForContext)

  def maybeThreadId: Option[String] = Some(info.channel)

  def eventualMaybeDMChannel(services: DefaultServices)(implicit actorSystem: ActorSystem, ec: ExecutionContext): Future[Option[String]] = {
    Future.successful(None)
  }

  def messageRecipientPrefix(isUninterruptedConversation: Boolean): String = ""

  def botName(services: DefaultServices)(implicit actorSystem: ActorSystem, ec: ExecutionContext): Future[String] = {
    services.dataService.msTeamsBotProfiles.maybeNameFor(profile).map { maybeName =>
      maybeName.getOrElse(fallbackBotPrefix)
    }
  }

  def maybeBotInfo(services: DefaultServices)(implicit actorSystem: ActorSystem, ec: ExecutionContext): Future[Option[BotInfo]] = {
    botName(services).map { botName =>
      Some(BotInfo(botName, botUserIdForContext))
    }
  }

  def maybeChannelDataForAction(services: DefaultServices)(implicit ec: ExecutionContext): DBIO[Option[Channel]] = {
    DBIO.successful(Some(Channel(channel, info.channelName, None, None))) // TODO: flesh this out
  }

  def maybeChannelForSendAction(
                                 responseType: BehaviorResponseType,
                                 maybeConversation: Option[Conversation],
                                 services: DefaultServices
                               )(implicit ec: ExecutionContext, actorSystem: ActorSystem): DBIO[Option[String]] = {
    DBIO.successful(None)
  }

  val maybeResponseUrl: Option[String] = info.maybeResponseUrl

  def sendMessage(
                   event: Event,
                   unformattedText: String,
                   maybeBehaviorVersion: Option[BehaviorVersion],
                   responseType: BehaviorResponseType,
                   maybeShouldUnfurl: Option[Boolean],
                   maybeConversation: Option[Conversation],
                   attachments: Seq[MessageAttachment],
                   files: Seq[UploadFileSpec],
                   choices: Seq[ActionChoice],
                   developerContext: DeveloperContext,
                   services: DefaultServices
                 )(implicit actorSystem: ActorSystem, ec: ExecutionContext): Future[Option[Message]] = {
    for {
      maybeDMChannel <- eventualMaybeDMChannel(services)
      botName <- botName(services)
      maybeResult <- MSTeamsMessageSender(
        services.msTeamsApiService.profileClientFor(profile),
        userIdForContext,
        profile.teamIdForContext,
        info,
        unformattedText,
        responseType,
        developerContext,
        channel,
        maybeDMChannel,
        maybeThreadId,
        maybeShouldUnfurl,
        maybeConversation,
        attachments,
        files,
        choices,
        botName,
        services,
        event.isEphemeral,
        event.beQuiet,
        maybeBehaviorVersion
      ).send
    } yield maybeResult
  }

  def channelDetailsFor(services: DefaultServices)(implicit actorSystem: ActorSystem, ec: ExecutionContext): Future[JsObject] = {
    val client = services.msTeamsApiService.profileClientFor(profile)
    for {
      maybeChannel <- info.channelData.channel.map { channel =>
        services.cacheService.getMSTeamsChannelFor(profile, channel.id)
      }.getOrElse(Future.successful(None))
      channelMembers <- maybeChannel.map { channel =>
        client.getTeamMembers(channel.team.id)
      }.getOrElse(Future.successful(Seq()))
    } yield {
      Json.obj(
        "channelName" -> JsString(maybeChannel.flatMap(_.channel.displayName).getOrElse("Private chat")),
        "channelMembers" -> channelMembers.map(_.id)
      )
    }
  }

  def maybeUserDetailsFor(services: DefaultServices)(implicit actorSystem: ActorSystem, ec: ExecutionContext): Future[Option[JsObject]] = {
    Future.successful(None) // TODO: this
  }

  def newRunEventFor(
                   botResult: BotResult,
                   nextAction: NextAction,
                   behaviorVersion: BehaviorVersion,
                   channel: String,
                   maybeMessageId: Option[String],
                   maybeThreadId: Option[String]
                 ): Event = {
    val eventContext = MSTeamsEventContext(
      profile,
      info
    )
    MSTeamsRunEvent(
      eventContext,
      behaviorVersion,
      nextAction.argumentsMap,
      EventType.nextAction,
      Some(botResult.event.originalEventType),
      maybeScheduled = None,
      botResult.event.isEphemeral,
      botResult.event.maybeResponseUrl,
      maybeMessageId
    )
  }

  def maybeOpenDialog(
                  event: Event,
                  dialog: Dialog,
                  developerContext: DeveloperContext,
                  services: DefaultServices
                )(implicit actorSystem: ActorSystem, ec: ExecutionContext): Future[Option[Boolean]] = {
    // TODO: Can MS Teams support dialogs?
    Future.successful(None)
  }

  def reactionHandler(eventualResults: Future[Seq[BotResult]], maybeMessageTs: Option[String], services: DefaultServices)
                     (implicit ec: ExecutionContext, actorSystem: ActorSystem): Future[Seq[BotResult]] = {
    MSTeamsMessageReactionHandler.handle(services.msTeamsApiService.profileClientFor(profile), eventualResults, info)
    eventualResults
  }

  def messageActionButtonFor(
                              callbackId: String,
                              label: String,
                              value: String,
                              maybeStyle: Option[String] = None
                            ) = {
    MSTeamsMessageActionButton(label, callbackId, Json.obj(callbackId -> value))
  }

  def messageActionMenuItemFor(
                                text: String,
                                value: String
                              ): MSTeamsMessageMenuItem = {
    MSTeamsMessageMenuItem(text, value)
  }

  def messageActionMenuFor(
                            name: String,
                            text: String,
                            options: Seq[MSTeamsMessageMenuItem]
                          ): MSTeamsMessageMenu = {
    MSTeamsMessageMenu(name, text, options)
  }

  def maybeMessageActionTextInputFor(
                                      name: String
                                    ): Option[MessageActionType] = {
    Some(MSTeamsMessageTextInput(name))
  }

  def messageAttachmentFor(
                            maybeText: Option[String] = None,
                            maybeUserDataList: Option[Set[UserData]] = None,
                            maybeTitle: Option[String] = None,
                            maybeTitleLink: Option[String] = None,
                            maybeColor: Option[Color] = None,
                            maybeCallbackId: Option[String] = None,
                            actions: Seq[MSTeamsMessageAction] = Seq()
                          ): MSTeamsMessageAttachment = {
    MSTeamsMessageAttachment(maybeText, maybeUserDataList, maybeTitle, maybeTitleLink, maybeColor, maybeCallbackId, actions)
  }

}

case class TestEventContext(
                             user: User,
                             team: Team
                           ) extends EventContext {

  override type MessageAttachmentType = SlackMessageAttachment
  override type MessageActionType = SlackMessageAction
  override type MenuType = SlackMessageMenu
  override type MenuItemType = SlackMessageMenuItem
  override type MessageEventType = SlackMessageEvent

  val name = "test"
  val description = "Test"
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

  def loginInfo: LoginInfo = LoginInfo(name, userIdForContext)
  def otherLoginInfos: Seq[LoginInfo] = Seq()

  override def ensureUserAction(dataService: DataService): DBIO[User] = DBIO.successful(user)
  def eventualMaybeDMChannel(services: DefaultServices)(implicit actorSystem: ActorSystem, ec: ExecutionContext) = Future.successful(None)

  def maybeChannelForSendAction(
                                 responseType: BehaviorResponseType,
                                 maybeConversation: Option[Conversation],
                                 services: DefaultServices
                               )(implicit ec: ExecutionContext, actorSystem: ActorSystem): DBIO[Option[String]] = DBIO.successful(None)

  def botName(services: DefaultServices)(implicit actorSystem: ActorSystem, ec: ExecutionContext): Future[String] = {
    Future.successful(s"${team.name} TestBot")
  }

  def maybeChannelDataForAction(services: DefaultServices)(implicit ec: ExecutionContext): DBIO[Option[Channel]] = {
    DBIO.successful(None)
  }

  def messageRecipientPrefix(isUninterruptedConversation: Boolean): String = ""

  def channelDetailsFor(services: DefaultServices)(implicit actorSystem: ActorSystem, ec: ExecutionContext): Future[JsObject] = {
    Future.successful(JsObject(Seq()))
  }
  def maybeUserDetailsFor(services: DefaultServices)(implicit actorSystem: ActorSystem, ec: ExecutionContext): Future[Option[JsObject]] = {
    Future.successful(None)
  }

  val messageBuffer: ArrayBuffer[String] = new ArrayBuffer()

  def sendMessage(
                   event: Event,
                   text: String,
                   maybeBehaviorVersion: Option[BehaviorVersion],
                   responseType: BehaviorResponseType,
                   maybeShouldUnfurl: Option[Boolean],
                   maybeConversation: Option[Conversation],
                   attachments: Seq[MessageAttachment],
                   files: Seq[UploadFileSpec],
                   choices: Seq[ActionChoice],
                   developerContext: DeveloperContext,
                   services: DefaultServices
                 )(implicit actorSystem: ActorSystem, ec: ExecutionContext): Future[Option[Message]] = {
    Future.successful(messageBuffer += text).map(_ => None)
  }

  def newRunEventFor(
                   botResult: BotResult,
                   nextAction: NextAction,
                   behaviorVersion: BehaviorVersion,
                   channel: String,
                   maybeMessageId: Option[String],
                   maybeThreadId: Option[String]
                 ): Event = {
    TestRunEvent(
      copy(),
      behaviorVersion,
      nextAction.argumentsMap,
      maybeScheduled = None
    )
  }

  def maybeOpenDialog(
                       event: Event,
                       dialog: Dialog,
                       developerContext: DeveloperContext,
                       services: DefaultServices
                     )(implicit actorSystem: ActorSystem, ec: ExecutionContext): Future[Option[Boolean]] = {
    Future.successful(None)
  }

  def reactionHandler(eventualResults: Future[Seq[BotResult]], maybeMessageTs: Option[String], services: DefaultServices)
                     (implicit ec: ExecutionContext, actorSystem: ActorSystem): Future[Seq[BotResult]] = {
    eventualResults
  }

  // TODO: we may have some reason to not use Slack here (although this was the existing behavior)

  def messageActionButtonFor(
                              callbackId: String,
                              label: String,
                              value: String,
                              maybeStyle: Option[String] = None
                            ) = {
    SlackMessageActionButton(callbackId, label, value)
  }

  def messageActionMenuItemFor(
                                text: String,
                                value: String
                              ): SlackMessageMenuItem = {
    SlackMessageMenuItem(text, value)
  }

  def messageActionMenuFor(
                            name: String,
                            text: String,
                            options: Seq[SlackMessageMenuItem]
                          ): SlackMessageMenu = {
    SlackMessageMenu(name, text, options)
  }

  def maybeMessageActionTextInputFor(
                                       name: String
                                     ): Option[MessageActionType] = None

  def messageAttachmentFor(
                            maybeText: Option[String] = None,
                            maybeUserDataList: Option[Set[UserData]] = None,
                            maybeTitle: Option[String] = None,
                            maybeTitleLink: Option[String] = None,
                            maybeColor: Option[Color] = None,
                            maybeCallbackId: Option[String] = None,
                            actions: Seq[SlackMessageAction] = Seq()
                          ): SlackMessageAttachment = {
    SlackMessageAttachment(maybeText, maybeUserDataList, maybeTitle, maybeTitleLink, maybeColor, maybeCallbackId, actions)
  }

}

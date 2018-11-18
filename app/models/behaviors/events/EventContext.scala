package models.behaviors.events

import akka.actor.ActorSystem
import com.mohiva.play.silhouette.api.LoginInfo
import json.Formatting._
import json.SlackUserData
import models.accounts.ms_teams.botprofile.MSTeamsBotProfile
import models.accounts.slack.botprofile.SlackBotProfile
import models.accounts.user.User
import models.behaviors._
import models.behaviors.behaviorversion.{BehaviorResponseType, BehaviorVersion, Private}
import models.behaviors.conversations.conversation.Conversation
import models.behaviors.events.ms_teams._
import models.behaviors.events.slack._
import models.behaviors.testing.TestRunEvent
import models.team.Team
import play.api.Logger
import play.api.libs.json.{JsObject, JsValue, Json}
import services.caching.SlackMessagePermalinkCacheKey
import services.ms_teams.apiModels.ActivityInfo
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

  val isPublicChannel: Boolean
  val isDirectMessage: Boolean
  val maybeChannel: Option[String]
  val name: String

  val usesTextInputs: Boolean = false

  val ellipsisTeamId: String
  val userIdForContext: String
  val teamIdForContext: String

  def maybeBotInfo(services: DefaultServices)(implicit actorSystem: ActorSystem, ec: ExecutionContext): Future[Option[BotInfo]]
  def eventualMaybeDMChannel(services: DefaultServices)(implicit actorSystem: ActorSystem, ec: ExecutionContext): Future[Option[String]]

  def loginInfo: LoginInfo = LoginInfo(name, userIdForContext)
  def ensureUserAction(dataService: DataService): DBIO[User] = {
    dataService.users.ensureUserForAction(loginInfo, ellipsisTeamId)
  }

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
                   attachments: Seq[MessageAttachment],
                   files: Seq[UploadFileSpec],
                   choices: Seq[ActionChoice],
                   developerContext: DeveloperContext,
                   services: DefaultServices
                 )(implicit actorSystem: ActorSystem, ec: ExecutionContext): Future[Option[String]]

  def newRunEventFor(
                   botResult: BotResult,
                   nextAction: NextAction,
                   behaviorVersion: BehaviorVersion,
                   channel: String,
                   maybeMessageId: Option[String]
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

  def messageActionTextInputFor(
                               name: String
                               ): MessageActionType

  def messageAttachmentFor(
                            maybeText: Option[String] = None,
                            maybeUserDataList: Option[Set[MessageUserData]] = None,
                            maybeTitle: Option[String] = None,
                            maybeTitleLink: Option[String] = None,
                            maybeColor: Option[String] = None,
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

  def maybeBotInfo(services: DefaultServices)(implicit actorSystem: ActorSystem, ec: ExecutionContext): Future[Option[BotInfo]] = {
    botName(services).map { botName =>
      Some(BotInfo(botName, profile.userId))
    }
  }

  val ellipsisTeamId: String = profile.teamId
  lazy val name: String = Conversation.SLACK_CONTEXT
  val maybeChannel: Option[String] = Some(channel)
  val teamIdForContext: String = profile.slackTeamId
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
                   attachments: Seq[MessageAttachment],
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
        attachments,
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
                    maybeMessageId: Option[String]
                  ): Event = {
    val eventContext = SlackEventContext(
      profile,
      channel,
      botResult.responseType.maybeThreadTsToUseForNextAction(botResult, channel, maybeMessageId),
      userIdForContext
    )
    SlackRunEvent(
      eventContext,
      behaviorVersion,
      nextAction.argumentsMap,
      Some(botResult.event.eventType),
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

  def messageActionTextInputFor(
                                 name: String
                               ): MessageActionType = ???

  def messageAttachmentFor(
                            maybeText: Option[String] = None,
                            maybeUserDataList: Option[Set[MessageUserData]] = None,
                            maybeTitle: Option[String] = None,
                            maybeTitleLink: Option[String] = None,
                            maybeColor: Option[String] = None,
                            maybeCallbackId: Option[String] = None,
                            actions: Seq[SlackMessageAction] = Seq()
                          ): SlackMessageAttachment = {
    SlackMessageAttachment(maybeText, maybeUserDataList, maybeTitle, maybeTitleLink, maybeColor, maybeCallbackId, actions)
  }

}

case class MSTeamsEventContext(
                              profile: MSTeamsBotProfile,
                              info: ActivityInfo
                              ) extends EventContext {

  override type MessageAttachmentType = MSTeamsMessageAttachment
  override type MessageActionType = MSTeamsMessageAction
  override type MenuType = MSTeamsMessageMenu
  override type MenuItemType = MSTeamsMessageMenuItem

  val name: String = Conversation.MS_TEAMS_CONTEXT
  val userIdForContext: String = info.from.id
  val teamIdForContext: String = profile.teamIdForContext
  val ellipsisTeamId: String = profile.teamId
  val botUserIdForContext: String = info.recipient.id

  override val usesTextInputs: Boolean = true

  val isDirectMessage: Boolean = {
    info.conversation.conversationType == "personal"
  }
  val isPublicChannel: Boolean = {
    info.conversation.conversationType == "team"
  }
  val channel: String = info.conversation.id
  val maybeChannel: Option[String] = Some(channel)

  def maybeBotUserIdForContext: Option[String] = Some(botUserIdForContext)

  def maybeUserIdForContext: Option[String] = Some(userIdForContext)

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
                   attachments: Seq[MessageAttachment],
                   files: Seq[UploadFileSpec],
                   choices: Seq[ActionChoice],
                   developerContext: DeveloperContext,
                   services: DefaultServices
                 )(implicit actorSystem: ActorSystem, ec: ExecutionContext): Future[Option[String]] = {
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
        event.messageUserDataList(maybeConversation, services),
        services,
        event.isEphemeral,
        event.beQuiet
      ).send
    } yield maybeResult
  }

  def detailsFor(services: DefaultServices)(implicit actorSystem: ActorSystem, ec: ExecutionContext): Future[JsObject] = {
    Future.successful(JsObject(Seq())) // TODO: this
  }

  def newRunEventFor(
                   botResult: BotResult,
                   nextAction: NextAction,
                   behaviorVersion: BehaviorVersion,
                   channel: String,
                   maybeMessageId: Option[String]
                 ): Event = {
    val eventContext = MSTeamsEventContext(
      profile,
      info
    )
    MSTeamsRunEvent(
      eventContext,
      behaviorVersion,
      nextAction.argumentsMap,
      Some(botResult.event.eventType),
      botResult.event.isEphemeral,
      botResult.event.maybeResponseUrl,
      maybeMessageId
    )
  }

  def reactionHandler(eventualResults: Future[Seq[BotResult]], maybeMessageTs: Option[String], services: DefaultServices)
                     (implicit ec: ExecutionContext, actorSystem: ActorSystem): Future[Seq[BotResult]] = {
    eventualResults // TODO: this
  }

  def messageActionButtonFor(
                              callbackId: String,
                              label: String,
                              value: String,
                              maybeStyle: Option[String] = None
                            ) = {
    MSTeamsMessageActionButton(label, Json.obj(callbackId -> value).toString())
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

  def messageActionTextInputFor(
                                 name: String
                               ): MessageActionType = {
    MSTeamsMessageTextInput(name)
  }

  def messageAttachmentFor(
                            maybeText: Option[String] = None,
                            maybeUserDataList: Option[Set[MessageUserData]] = None,
                            maybeTitle: Option[String] = None,
                            maybeTitleLink: Option[String] = None,
                            maybeColor: Option[String] = None,
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
                   attachments: Seq[MessageAttachment],
                   files: Seq[UploadFileSpec],
                   choices: Seq[ActionChoice],
                   developerContext: DeveloperContext,
                   services: DefaultServices
                 )(implicit actorSystem: ActorSystem, ec: ExecutionContext): Future[Option[String]] = {
    Future.successful(messageBuffer += text).map(_ => None)
  }

  def newRunEventFor(
                   botResult: BotResult,
                   nextAction: NextAction,
                   behaviorVersion: BehaviorVersion,
                   channel: String,
                   maybeMessageId: Option[String]
                 ): Event = {
    TestRunEvent(
      copy(),
      behaviorVersion,
      nextAction.argumentsMap
    )
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

  def messageActionTextInputFor(
                                 name: String
                               ): MessageActionType = ???

  def messageAttachmentFor(
                            maybeText: Option[String] = None,
                            maybeUserDataList: Option[Set[MessageUserData]] = None,
                            maybeTitle: Option[String] = None,
                            maybeTitleLink: Option[String] = None,
                            maybeColor: Option[String] = None,
                            maybeCallbackId: Option[String] = None,
                            actions: Seq[SlackMessageAction] = Seq()
                          ): SlackMessageAttachment = {
    SlackMessageAttachment(maybeText, maybeUserDataList, maybeTitle, maybeTitleLink, maybeColor, maybeCallbackId, actions)
  }

}

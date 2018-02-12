package models.behaviors.events

import akka.actor.ActorSystem
import models.accounts.slack.botprofile.SlackBotProfile
import models.accounts.user.User
import models.behaviors.conversations.conversation.Conversation
import play.api.Configuration
import services.{AWSLambdaService, CacheService, DataService, DefaultServices}
import slack.api.SlackApiClient
import utils.{SlackMessageSender, UploadFileSpec}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.matching.Regex

case class SlackMessageEvent(
                              profile: SlackBotProfile,
                              channel: String,
                              maybeThreadId: Option[String],
                              user: String,
                              message: SlackMessage,
                              maybeFile: Option[SlackFile],
                              ts: String,
                              client: SlackApiClient,
                              maybeOriginalEventType: Option[EventType]
                            ) extends MessageEvent with SlackEvent {

  val eventType: EventType = EventType.chat

  def withOriginalEventType(originalEventType: EventType): Event = {
    this.copy(maybeOriginalEventType = Some(originalEventType))
  }

  lazy val isBotMessage: Boolean = profile.userId == user

  override def botPrefix(services: DefaultServices)(implicit actorSystem: ActorSystem, ec: ExecutionContext): Future[String] = {
    if (isDirectMessage) {
      Future.successful("")
    } else {
      for {
        maybeSlackUserData <- services.slackEventService.maybeSlackUserDataFor(profile)
      } yield {
        maybeSlackUserData.map { userData =>
          s"@${userData.displayName} "
        }.getOrElse("...")
      }
    }
  }

  val messageText: String = message.originalText

  override val relevantMessageTextWithFormatting: String = {
    message.withoutBotPrefix
  }

  override val relevantMessageText: String = {
    message.unformattedText
  }

  lazy val includesBotMention: Boolean = {
    isDirectMessage ||
      SlackMessageEvent.mentionRegexFor(profile.userId).findFirstMatchIn(message.originalText).nonEmpty ||
      MessageEvent.ellipsisRegex.findFirstMatchIn(message.originalText).nonEmpty
  }

  override val isResponseExpected: Boolean = includesBotMention
  val teamId: String = profile.teamId
  val userIdForContext: String = user

  lazy val maybeChannel = Some(channel)
  lazy val name: String = Conversation.SLACK_CONTEXT

  def maybeOngoingConversation(dataService: DataService)(implicit ec: ExecutionContext): Future[Option[Conversation]] = {
    dataService.conversations.findOngoingFor(user, context, maybeChannel, maybeThreadId).flatMap { maybeConvo =>
      maybeConvo.map(c => Future.successful(Some(c))).getOrElse(maybeConversationRootedHere(dataService))
    }
  }

  def maybeConversationRootedHere(dataService: DataService): Future[Option[Conversation]] = {
    dataService.conversations.findOngoingFor(user, context, maybeChannel, Some(ts))
  }

  override def recentMessages(dataService: DataService)(implicit actorSystem: ActorSystem, ec: ExecutionContext): Future[Seq[String]] = {
    for {
      maybeTeam <- dataService.teams.find(profile.teamId)
      maybeOAuthToken <- dataService.oauth2Tokens.maybeFullForSlackTeamId(profile.slackTeamId)
      maybeUserClient <- Future.successful(maybeOAuthToken.map { token =>
        SlackApiClient(token.accessToken)
      })
      maybeHistory <- maybeUserClient.map { userClient =>
        userClient.getChannelHistory(channel, latest = Some(ts)).map(Some(_))
      }.getOrElse(Future.successful(None))
      messages <- Future.successful(maybeHistory.map { history =>
        history.messages.slice(0, 10).reverse.flatMap { json =>
          (json \ "text").asOpt[String]
        }
      }.getOrElse(Seq()))
    } yield messages
  }

  override def navLinks(lambdaService: AWSLambdaService): String = {
    navLinkList(lambdaService).map { case(title, path) =>
      s"[$title]($path)"
    }.mkString("  ·  ")
  }

  def channelForSend(
                      forcePrivate: Boolean,
                      maybeConversation: Option[Conversation],
                      cacheService: CacheService
                    )(implicit actorSystem: ActorSystem, ec: ExecutionContext): Future[String] = {
    (if (forcePrivate) {
      eventualMaybeDMChannel(cacheService)
    } else {
      Future.successful(maybeConversation.flatMap(_.maybeChannel))
    }).map { maybeChannel =>
      maybeChannel.getOrElse(channel)
    }
  }

  def sendMessage(
                   unformattedText: String,
                   forcePrivate: Boolean,
                   maybeShouldUnfurl: Option[Boolean],
                   maybeConversation: Option[Conversation],
                   attachmentGroups: Seq[MessageAttachmentGroup],
                   files: Seq[UploadFileSpec],
                   isForUndeployed: Boolean,
                   cacheService: CacheService,
                   configuration: Configuration
                 )(implicit actorSystem: ActorSystem, ec: ExecutionContext): Future[Option[String]] = {
    channelForSend(forcePrivate, maybeConversation, cacheService).flatMap { channelToUse =>
      SlackMessageSender(
        client,
        user,
        profile.slackTeamId,
        unformattedText,
        forcePrivate,
        isForUndeployed,
        channel,
        channelToUse,
        maybeThreadId,
        maybeShouldUnfurl,
        maybeConversation,
        attachmentGroups,
        files,
        configuration
      ).send
    }
  }

  override def ensureUser(dataService: DataService)(implicit ec: ExecutionContext): Future[User] = {
    super.ensureUser(dataService).flatMap { user =>
      ensureSlackProfileFor(loginInfo, dataService).map(_ => user)
    }
  }

}

object SlackMessageEvent {

  def mentionRegexFor(botId: String): Regex = s"""<@$botId>""".r
  def toBotRegexFor(botId: String): Regex = s"""^<@$botId>:?\\s*""".r

}

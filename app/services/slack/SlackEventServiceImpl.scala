package services.slack

import akka.actor.ActorSystem
import javax.inject._
import json.{SlackUserData, SlackUserProfileData}
import models.accounts.slack.botprofile.SlackBotProfile
import models.behaviors.BotResultService
import models.behaviors.events.{EventHandler, SlackMessageEvent}
import play.api.Logger
import play.api.i18n.MessagesApi
import services.DataService
import services.caching.{CacheService, SlackUserDataCacheKey}
import services.slack.apiModels.SlackUser

import scala.concurrent.{ExecutionContext, Future}

class SlackEventServiceImpl @Inject()(
                                   val dataService: DataService,
                                   val cacheService: CacheService,
                                   messages: MessagesApi,
                                   val eventHandler: EventHandler,
                                   val botResultService: BotResultService,
                                   val slackApiService: SlackApiService,
                                   implicit val actorSystem: ActorSystem
                                 ) extends SlackEventService {

  implicit val ec: ExecutionContext = actorSystem.dispatcher

  def onEvent(event: SlackMessageEvent): Future[Unit] = {
    if (!event.isBotMessage) {
      for {
        maybeConversation <- event.maybeOngoingConversation(dataService)
        _ <- eventHandler.handle(event, maybeConversation).flatMap { results =>
          Future.sequence(
            results.map(result => botResultService.sendIn(result, None).map { _ =>
              Logger.info(event.logTextFor(result, None))
            })
          )
        }
      } yield {}
    } else {
      Future.successful({})
    }
  }

  def clientFor(botProfile: SlackBotProfile): SlackApiClient = slackApiService.clientFor(botProfile)

  def slackUserDataList(slackUserIds: Set[String], botProfile: SlackBotProfile): Future[Set[SlackUserData]] = {
    val client = clientFor(botProfile)
    val slackTeamId = botProfile.slackTeamId
    Future.sequence(slackUserIds.map { userId =>
      maybeSlackUserDataFor(userId, slackTeamId, client, (e) => {
        Logger.info(
          s"""Slack API reported user not found while trying to convert user IDs to username:
            |Slack user ID: ${userId}
            |Ellipsis bot Slack team ID: ${botProfile.slackTeamId}
            |Ellipsis team ID: ${botProfile.teamId}
          """.stripMargin, e)
        None
      })
    }).map(_.flatten)
  }

  def fetchSlackUserDataFn(slackUserId: String, slackTeamId: String, client: SlackApiClient, onUserNotFound: (SlackApiError => Option[SlackUser])): SlackUserDataCacheKey => Future[Option[SlackUserData]] = {
    key: SlackUserDataCacheKey => {
      for {
        maybeInfo <- client.getUserInfo(key.slackUserId)
      } yield {
        maybeInfo.map { info =>
          val maybeProfile = info.profile.map { profile =>
            SlackUserProfileData(
              profile.display_name,
              profile.first_name,
              profile.last_name,
              profile.real_name,
              profile.email,
              profile.phone
            )
          }
          SlackUserData(
            key.slackUserId,
            info.team_id.getOrElse(slackTeamId),
            info.name,
            isPrimaryOwner = info.is_primary_owner.getOrElse(false),
            isOwner = info.is_owner.getOrElse(false),
            isRestricted = info.is_restricted.getOrElse(false),
            isUltraRestricted = info.is_ultra_restricted.getOrElse(false),
            isBot = info.is_bot.getOrElse(false),
            tz = info.tz,
            info.deleted.getOrElse(false),
            maybeProfile
          )
        }
      }
    }
  }

  def maybeSlackUserDataFor(slackUserId: String, slackTeamId: String, client: SlackApiClient, onUserNotFound: SlackApiError => Option[SlackUser]): Future[Option[SlackUserData]] = {
    cacheService.getSlackUserData(SlackUserDataCacheKey(slackUserId, slackTeamId), fetchSlackUserDataFn(slackUserId, slackTeamId, client, onUserNotFound))
  }

  def maybeSlackUserDataFor(botProfile: SlackBotProfile): Future[Option[SlackUserData]] = {
    maybeSlackUserDataFor(botProfile.userId, botProfile.slackTeamId, clientFor(botProfile), (e) => {
      Logger.error(s"Slack said the Ellipsis bot Slack user could not be found for Ellipsis team ${botProfile.teamId} on Slack team ${botProfile.slackTeamId} with slack user ID ${botProfile.userId}", e)
      None
    })
  }
}

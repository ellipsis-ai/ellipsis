package services.slack

import akka.actor.ActorSystem
import javax.inject._
import json.{SlackUserData, SlackUserProfileData}
import models.accounts.linkedaccount.LinkedAccount
import models.accounts.slack.SlackUserTeamIds
import models.accounts.slack.botprofile.SlackBotProfile
import models.behaviors.BotResultService
import models.behaviors.events.{Event, EventHandler}
import play.api.Logger
import play.api.i18n.MessagesApi
import services.DataService
import services.caching.{CacheService, SlackUserDataByEmailCacheKey, SlackUserDataCacheKey}
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

  def onEvent(event: Event): Future[Unit] = {
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
    Future.sequence(slackUserIds.map { userId =>
      maybeSlackUserDataFor(userId, client, (e) => {
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

  private def slackUserDataFromSlackUser(user: SlackUser, client: SlackApiClient): SlackUserData = {
    val maybeProfile = user.profile.map { profile =>
      SlackUserProfileData(
        profile.display_name,
        profile.first_name,
        profile.last_name,
        profile.real_name,
        profile.email,
        profile.phone
      )
    }
    val maybeTeams = user.enterprise_user.flatMap(_.teams)
    val firstTeam = user.team_id.
      orElse(maybeTeams.flatMap(_.headOption)).
      getOrElse(client.profile.slackTeamId)
    val otherTeams = maybeTeams.filter(_ != firstTeam).getOrElse(Seq.empty)
    SlackUserData(
      user.id,
      client.profile.maybeSlackEnterpriseId,
      SlackUserTeamIds(firstTeam, otherTeams),
      user.name,
      isPrimaryOwner = user.is_primary_owner.getOrElse(false),
      isOwner = user.is_owner.getOrElse(false),
      isRestricted = user.is_restricted.getOrElse(false),
      isUltraRestricted = user.is_ultra_restricted.getOrElse(false),
      isBot = user.is_bot.getOrElse(false),
      tz = user.tz,
      user.deleted.getOrElse(false),
      maybeProfile
    )
  }

  def fetchSlackUserDataFn(slackUserId: String, slackTeamId: String, client: SlackApiClient, onUserNotFound: (SlackApiError => Option[SlackUser])): SlackUserDataCacheKey => Future[Option[SlackUserData]] = {
    key: SlackUserDataCacheKey => {
      for {
        maybeInfo <- client.getUserInfo(key.slackUserId).map { maybeSlackUser =>
          maybeSlackUser.foreach { slackUser =>
            cacheService.cacheFallbackSlackUser(slackUserId, slackTeamId, slackUser)
          }
          maybeSlackUser
        }.recover {
          case e: InvalidResponseException => {
            Logger.error(s"Invalid response while fetching user info for Slack user ID ${slackUserId} on Slack team ${slackTeamId}. Trying fallback cache...", e)
            val maybeSlackUser = cacheService.getFallbackSlackUser(slackUserId, slackTeamId)
            if (maybeSlackUser.isDefined) {
              Logger.error(s"Slack user ID ${slackUserId} on Slack team ${slackTeamId} found in fallback cache")
              maybeSlackUser
            } else {
              Logger.error(s"Slack user ID ${slackUserId} on Slack team ${slackTeamId} not found in fallback cache. Giving up.")
              throw e
            }
          }
        }
      } yield {
        maybeInfo.map(info => slackUserDataFromSlackUser(info, client))
      }
    }
  }

  def maybeSlackUserDataFor(slackUserId: String, client: SlackApiClient, onUserNotFound: SlackApiError => Option[SlackUser]): Future[Option[SlackUserData]] = {
    val slackTeamId = client.profile.slackTeamId
    cacheService.getSlackUserData(SlackUserDataCacheKey(slackUserId, slackTeamId), fetchSlackUserDataFn(slackUserId, slackTeamId, client, onUserNotFound))
  }

  def maybeSlackUserDataFor(botProfile: SlackBotProfile): Future[Option[SlackUserData]] = {
    maybeSlackUserDataFor(botProfile.userId, clientFor(botProfile), (e) => {
      Logger.error(s"Slack said the Ellipsis bot Slack user could not be found for Ellipsis team ${botProfile.teamId} on Slack team ${botProfile.slackTeamId} with slack user ID ${botProfile.userId}", e)
      None
    })
  }

  def maybeSlackUserDataForEmail(email: String, client: SlackApiClient): Future[Option[SlackUserData]] = {
    cacheService.getSlackUserDataByEmail(SlackUserDataByEmailCacheKey(email, client.profile.slackTeamId), fetchSlackUserDataByEmailFn(email, client))
  }

  def fetchSlackUserDataByEmailFn(email: String, client: SlackApiClient): SlackUserDataByEmailCacheKey => Future[Option[SlackUserData]] = {
    key: SlackUserDataByEmailCacheKey => {
      for {
        maybeInfo <- client.getUserInfoByEmail(key.email)
      } yield {
        maybeInfo.map(info => slackUserDataFromSlackUser(info, client))
      }
    }
  }

  def isUserValidForBot(slackUserId: String, botProfile: SlackBotProfile, maybeEnterpriseId: Option[String]): Future[Boolean] = {
    cacheService.getSlackUserIsValidForBotTeam(slackUserId, botProfile, maybeEnterpriseId).map(Future.successful).getOrElse {
      for {
        maybeUserData <- maybeSlackUserDataFor(slackUserId, clientFor(botProfile), _ => None)
      } yield {
        maybeUserData.exists { userData =>
          val userIsBot = userData.isBot
          val isEnterpriseGrid = maybeEnterpriseId.isDefined
          val userIsOnTeam = userData.accountTeamIds.contains(botProfile.slackTeamId)
          val userIsAdmin = userData.accountTeamIds.contains(LinkedAccount.ELLIPSIS_SLACK_TEAM_ID)
          val userIsValid = !userIsBot && (isEnterpriseGrid || userIsOnTeam || userIsAdmin)
          cacheService.cacheSlackUserIsValidForBotTeam(slackUserId, botProfile, maybeEnterpriseId, userIsValid)
          userIsValid
        }
      }
    }
  }
}

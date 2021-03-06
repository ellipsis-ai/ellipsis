package services.slack

import akka.actor.ActorSystem
import javax.inject._
import json.SlackUserData
import models.accounts.slack.botprofile.SlackBotProfile
import models.behaviors.BotResultService
import models.behaviors.events.{Event, EventHandler}
import play.api.Logger
import play.api.i18n.MessagesApi
import services.DataService
import services.caching.{CacheService, SlackUserDataByEmailCacheKey, SlackUserDataCacheKey}
import services.slack.apiModels.SlackUser
import slick.dbio.DBIO
import utils.FutureSequencer

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
    FutureSequencer.sequence(slackUserIds.toSeq, { (userId: String) =>
      maybeSlackUserDataFor(userId, client, (e) => {
        Logger.info(
          s"""Slack API reported user not found while trying to convert user IDs to username:
            |Slack user ID: ${userId}
            |Ellipsis bot Slack team ID: ${botProfile.slackTeamId}
            |Ellipsis team ID: ${botProfile.teamId}
          """.stripMargin, e)
        None
      })
    }).map(_.flatten.toSet)
  }

  private def slackUserDataFromSlackUser(user: SlackUser, client: SlackApiClient): SlackUserData = {
    SlackUserData.fromSlackUser(user, client.profile)
  }

  def fetchSlackUserDataFn(slackUserId: String, slackTeamId: String, client: SlackApiClient, onUserNotFound: (SlackApiError => Option[SlackUser])): SlackUserDataCacheKey => Future[Option[SlackUserData]] = {
    key: SlackUserDataCacheKey => {
      for {
        maybeInfo <- client.getUserInfo(key.slackUserId).map { maybeSlackUser =>
          maybeSlackUser.foreach { slackUser =>
            cacheService.cacheFallbackSlackUser(slackUserId, slackTeamId, slackUser)
          }
          maybeSlackUser
        }.recoverWith {
          case e: InvalidResponseException => {
            Logger.error(s"Invalid response while fetching user info for Slack user ID ${slackUserId} on Slack team ${slackTeamId}. Trying fallback cache...", e)
            cacheService.getFallbackSlackUser(slackUserId, slackTeamId).map { maybeSlackUser =>
              if (maybeSlackUser.isDefined) {
                Logger.error(s"Slack user ID ${slackUserId} on Slack team ${slackTeamId} found in fallback cache")
                maybeSlackUser
              } else {
                Logger.error(s"Slack user ID ${slackUserId} on Slack team ${slackTeamId} not found in fallback cache. Giving up.")
                throw e
              }
            }
          }
        }
      } yield {
        maybeInfo.map(info => slackUserDataFromSlackUser(info, client))
      }
    }
  }

  def maybeSlackUserDataForAction(slackUserId: String, client: SlackApiClient, onUserNotFound: SlackApiError => Option[SlackUser]): DBIO[Option[SlackUserData]] = {
    val slackTeamId = client.profile.slackTeamId
    DBIO.from(
      cacheService.getSlackUserData(SlackUserDataCacheKey(slackUserId, slackTeamId), fetchSlackUserDataFn(slackUserId, slackTeamId, client, onUserNotFound)).recover {
        case e: InvalidResponseException => {
          Logger.warn(s"Couldn’t retrieve bot user data from Slack API for ${client.profile.botDebugInfo} because of an invalid/error response; using fallback cache", e)
          None
        }
      }
    )
  }

  def maybeSlackUserDataFor(slackUserId: String, client: SlackApiClient, onUserNotFound: SlackApiError => Option[SlackUser]): Future[Option[SlackUserData]] = {
    dataService.run(maybeSlackUserDataForAction(slackUserId, client, onUserNotFound))
  }

  def maybeSlackUserDataForAction(botProfile: SlackBotProfile): DBIO[Option[SlackUserData]] = {
    maybeSlackUserDataForAction(botProfile.userId, clientFor(botProfile), (e) => {
      Logger.error(s"Slack said the Ellipsis bot Slack user could not be found for Ellipsis team ${botProfile.teamId} on Slack team ${botProfile.slackTeamId} with slack user ID ${botProfile.userId}", e)
      None
    })
  }

  def maybeSlackUserDataFor(botProfile: SlackBotProfile): Future[Option[SlackUserData]] = {
    dataService.run(maybeSlackUserDataForAction(botProfile))
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
    cacheService.getSlackUserIsValidForBotTeam(slackUserId, botProfile, maybeEnterpriseId).flatMap { maybeCachedValid =>
      maybeCachedValid.map(Future.successful).getOrElse {
        for {
          maybeUserData <- maybeSlackUserDataFor(slackUserId, clientFor(botProfile), _ => None)
        } yield {
          maybeUserData.exists { userData =>
            val userIsValid = userData.canTriggerBot(botProfile, maybeEnterpriseId)
            cacheService.cacheSlackUserIsValidForBotTeam(slackUserId, botProfile, maybeEnterpriseId, userIsValid)
            userIsValid
          }
        }
      }
    }
  }
}

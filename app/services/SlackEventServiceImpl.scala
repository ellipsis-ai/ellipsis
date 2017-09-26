package services

import javax.inject._

import akka.actor.ActorSystem
import json.SlackUserData
import models.accounts.slack.botprofile.SlackBotProfile
import models.behaviors.BotResultService
import models.behaviors.events.{EventHandler, SlackMessageEvent}
import play.api.Logger
import play.api.i18n.MessagesApi
import play.api.libs.json._
import slack.api.{ApiError, SlackApiClient}
import utils.SlackMessageReactionHandler

import scala.concurrent.{ExecutionContext, Future}

class SlackEventServiceImpl @Inject()(
                                   val dataService: DataService,
                                   val cacheService: CacheService,
                                   messages: MessagesApi,
                                   val eventHandler: EventHandler,
                                   val botResultService: BotResultService,
                                   implicit val actorSystem: ActorSystem
                                 ) extends SlackEventService {

  implicit val ec: ExecutionContext = actorSystem.dispatcher

  def onEvent(event: SlackMessageEvent): Future[Unit] = {
    if (!event.isBotMessage) {
      val eventuallyHandleMessage = for {
        maybeConversation <- event.maybeOngoingConversation(dataService)
        _ <- eventHandler.handle(event, maybeConversation).flatMap { results =>
          Future.sequence(
            results.map(result => botResultService.sendIn(result, None).map { _ =>
              Logger.info(event.logTextFor(result))
            })
          )
        }
      } yield {}
      SlackMessageReactionHandler.handle(event.client, eventuallyHandleMessage, event.channel, event.ts)
    } else {
      Future.successful({})
    }
  }

  def clientFor(botProfile: SlackBotProfile): SlackApiClient = SlackApiClient(botProfile.token)

  def slackUserDataList(slackUserIds: Set[String], botProfile: SlackBotProfile): Future[Set[SlackUserData]] = {
    val client = SlackApiClient(botProfile.token)
    val slackTeamId = botProfile.slackTeamId
    Future.sequence(slackUserIds.map { userId =>
      maybeSlackUserDataFor(userId, slackTeamId, client)
    }).map(_.flatten)
  }

  def maybeSlackUserDataFor(slackUserId: String, slackTeamId: String, client: SlackApiClient): Future[Option[SlackUserData]] = {
    cacheService.getSlackUserData(slackUserId, slackTeamId).map { userData =>
      Future.successful(Some(userData))
    }.getOrElse {
      for {
        maybeInfo <- client.getUserInfo(slackUserId).map(Some(_)).recover {
          case e: ApiError => None
        }
      } yield {
        maybeInfo.map { info =>
          val profileNameData = info.profile.map { profile =>
            Seq(
              profile.first_name.map(v => "firstName" -> JsString(v)),
              profile.last_name.map(v => "lastName" -> JsString(v)),
              profile.real_name.map(v => "realName" -> JsString(v))
            ).flatten
          }.getOrElse(Seq())
          val profileData = JsObject(
            Seq(
              "name" -> JsString(info.name),
              "profile" -> JsObject(profileNameData),
              "isPrimaryOwner" -> JsBoolean(info.is_primary_owner.getOrElse(false)),
              "isOwner" -> JsBoolean(info.is_owner.getOrElse(false)),
              "isRestricted" -> JsBoolean(info.is_restricted.getOrElse(false)),
              "isUltraRestricted" -> JsBoolean(info.is_ultra_restricted.getOrElse(false)),
              "tz" -> info.tz.map(JsString).getOrElse(JsNull)
            )
          )
          val userData = SlackUserData(slackUserId, slackTeamId, info.name, info.tz, profileData)
          cacheService.cacheSlackUserData(userData)
          Some(userData)
        }.getOrElse(None)
      }
    }
  }

  def maybeSlackUserDataFor(botProfile: SlackBotProfile): Future[Option[SlackUserData]] = {
    maybeSlackUserDataFor(botProfile.userId, botProfile.slackTeamId, clientFor(botProfile))
  }
}
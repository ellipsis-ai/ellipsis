package services.caching

import javax.inject.{Inject, Provider, Singleton}

import akka.actor.ActorSystem
import akka.http.caching.LfuCache
import akka.http.caching.scaladsl.{Cache, CachingSettings}
import json.Formatting._
import json.{ImmutableBehaviorGroupVersionData, SlackUserData}
import models.accounts.slack.botprofile.SlackBotProfile
import models.behaviors.BotResult
import models.behaviors.behaviorparameter.ValidValue
import models.behaviors.events._
import play.api.cache.SyncCacheApi
import play.api.libs.json.{JsError, JsSuccess, JsValue, Json}
import services._
import slack.models.{Channel, Group, Im}

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.reflect.ClassTag

case class SlackMessageEventData(
                                  profile: SlackBotProfile,
                                  userSlackTeamId: String,
                                  channel: String,
                                  maybeThreadId: Option[String],
                                  user: String,
                                  message: SlackMessage,
                                  maybeFile: Option[SlackFile],
                                  ts: String,
                                  maybeOriginalEventType: Option[String]
                                )

@Singleton
class CacheServiceImpl @Inject() (
                                   cache: SyncCacheApi, // TODO: change to async
                                   slackEventServiceProvider: Provider[SlackEventService],
                                   implicit val actorSystem: ActorSystem
                                 ) extends CacheService {

  def slackEventService = slackEventServiceProvider.get

  def cacheSettingsWithTimeToLive(duration: Duration): CachingSettings = {
    val defaultCachingSettings = CachingSettings(actorSystem)
    val shortLivedLfuCacheSettings =
      defaultCachingSettings.lfuCacheSettings
        .withTimeToLive(duration)
        .withTimeToIdle(duration.div(2))
    defaultCachingSettings.withLfuCacheSettings(shortLivedLfuCacheSettings)
  }

  val slackApiCallExpiry: Duration = 10.seconds
  val dataTypeBotResultsExpiry: Duration = 6.seconds

  def set[T: ClassTag](key: String, value: T, expiration: Duration = Duration.Inf): Unit = {
    cache.set(key, value, expiration)
  }

  def get[T : ClassTag](key: String): Option[T] = {
    cache.get[T](key)
  }

  def hasKey(key: String): Boolean = {
    cache.get(key).isDefined
  }

  def remove(key: String) = {
    cache.remove(key)
  }

  def cacheEvent(key: String, event: Event, expiration: Duration = Duration.Inf): Unit = {
    event match {
      case ev: SlackMessageEvent => {
        val eventData = SlackMessageEventData(ev.profile, ev.userSlackTeamId, ev.channel, ev.maybeThreadId, ev.user, ev.message, ev.maybeFile, ev.ts, ev.maybeOriginalEventType.map(_.toString))
        set(key, Json.toJson(eventData), expiration)
      }
      case _ =>
    }
  }

  def getEvent(key: String): Option[SlackMessageEvent] = {
    get[JsValue](key).flatMap { eventJson =>
      eventJson.validate[SlackMessageEventData] match {
        case JsSuccess(event, jsPath) => {
          Some(SlackMessageEvent(
            event.profile,
            event.userSlackTeamId,
            event.channel,
            event.maybeThreadId,
            event.user,
            event.message,
            event.maybeFile,
            event.ts,
            slackEventService.clientFor(event.profile),
            EventType.maybeFrom(event.maybeOriginalEventType)
          ))
        }
        case JsError(err) => None
      }
    }
  }

  def cacheValidValues(key: String, values: Seq[ValidValue], expiration: Duration = Duration.Inf): Unit = {
    set(key, Json.toJson(values), expiration)
  }

  def getValidValues(key: String): Option[Seq[ValidValue]] = {
    get[JsValue](key).flatMap { json =>
      json.validate[Seq[ValidValue]] match {
        case JsSuccess(values, jsPath) => Some(values)
        case JsError(err) => None
      }
    }
  }

  private val dataTypeBotResultsCache = LfuCache[DataTypeBotResultsCacheKey, BotResult](cacheSettingsWithTimeToLive(dataTypeBotResultsExpiry))

  def getDataTypeBotResult(key: DataTypeBotResultsCacheKey, dataFn: DataTypeBotResultsCacheKey => Future[BotResult]): Future[BotResult] = {
    dataTypeBotResultsCache.getOrLoad(key, dataFn)
  }

  private val slackChannelInfoCache = LfuCache[SlackChannelDataCacheKey, Option[Channel]](cacheSettingsWithTimeToLive(slackApiCallExpiry))

  def getSlackChannelInfo(key: SlackChannelDataCacheKey, dataFn: SlackChannelDataCacheKey => Future[Option[Channel]]): Future[Option[Channel]] = {
    slackChannelInfoCache.getOrLoad(key, dataFn)
  }

  private val slackGroupInfoCache = LfuCache[SlackGroupDataCacheKey, Option[Group]](cacheSettingsWithTimeToLive(slackApiCallExpiry))

  def getSlackGroupInfo(key: SlackGroupDataCacheKey, dataFn: SlackGroupDataCacheKey => Future[Option[Group]]): Future[Option[Group]] = {
    slackGroupInfoCache.getOrLoad(key, dataFn)
  }

  private val slackChannelsCache = LfuCache[String, Seq[Channel]](cacheSettingsWithTimeToLive(slackApiCallExpiry))

  def getSlackChannels(teamId: String, dataFn: String => Future[Seq[Channel]]): Future[Seq[Channel]] = {
    slackChannelsCache.getOrLoad(teamId, dataFn)
  }

  private val slackGroupsCache: Cache[String, Seq[Group]] = LfuCache(cacheSettingsWithTimeToLive(slackApiCallExpiry))

  def getSlackGroups(teamId: String, dataFn: String => Future[Seq[Group]]): Future[Seq[Group]] = {
    slackGroupsCache.getOrLoad(teamId, dataFn)
  }

  private val slackImsCache: Cache[String, Seq[Im]] = LfuCache(cacheSettingsWithTimeToLive(slackApiCallExpiry))

  def getSlackIMs(teamId: String, dataFn: String => Future[Seq[Im]]): Future[Seq[Im]] = {
    slackImsCache.getOrLoad(teamId, dataFn)
  }

  private val slackUserDataCache: Cache[SlackUserDataCacheKey, Option[SlackUserData]] = LfuCache(cacheSettingsWithTimeToLive(slackApiCallExpiry))

  def getSlackUserData(key: SlackUserDataCacheKey, dataFn: SlackUserDataCacheKey => Future[Option[SlackUserData]]): Future[Option[SlackUserData]] = {
    slackUserDataCache.getOrLoad(key, dataFn)
  }

  def cacheBehaviorGroupVersionData(data: ImmutableBehaviorGroupVersionData): Unit = {
    set(data.id, Json.toJson(data))
  }

  def getBehaviorGroupVersionData(groupVersionId: String): Option[ImmutableBehaviorGroupVersionData] = {
    get[JsValue](groupVersionId).flatMap { json =>
      json.validate[ImmutableBehaviorGroupVersionData] match {
        case JsSuccess(data, _) => Some(data)
        case JsError(_) => None
      }
    }
  }

  private def botNameKey(teamId: String): String = {
    s"team-$teamId-bot-name-v1"
  }

  def cacheBotName(name: String, teamId: String): Unit = {
    set(botNameKey(teamId), name, Duration.Inf)
  }

  def getBotName(teamId: String): Option[String] = {
    get(botNameKey(teamId))
  }

}

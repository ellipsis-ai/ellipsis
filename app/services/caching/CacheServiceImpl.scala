package services.caching

import java.nio.ByteBuffer

import akka.actor.ActorSystem
import akka.http.caching.LfuCache
import akka.http.caching.scaladsl.{Cache, CachingSettings}
import com.amazonaws.services.lambda.model.InvokeResult
import javax.inject.{Inject, Provider, Singleton}
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
                                  maybeOriginalEventType: Option[String],
                                  isUninterruptedConversation: Boolean
                                )

case class InvokeResultData(
                            statusCode: Int,
                            logResult: String,
                            payload: Array[Byte]
                           )

@Singleton
class CacheServiceImpl @Inject() (
                                   cache: SyncCacheApi, // TODO: change to async
                                   slackEventServiceProvider: Provider[SlackEventService],
                                   implicit val actorSystem: ActorSystem
                                 ) extends CacheService {

  val MAX_KEY_LENGTH = 250

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
  val dataTypeBotResultsExpiry: Duration = 24.hours

  def set[T: ClassTag](key: String, value: T, expiration: Duration = Duration.Inf): Unit = {
    if (key.getBytes().length <= MAX_KEY_LENGTH) {
      cache.set(key, value, expiration)
    }
  }

  def get[T : ClassTag](key: String): Option[T] = {
    if (key.getBytes().length <= MAX_KEY_LENGTH) {
      cache.get[T](key)
    } else {
      None
    }
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
        val eventData = SlackMessageEventData(ev.profile, ev.userSlackTeamId, ev.channel, ev.maybeThreadId, ev.user, ev.message, ev.maybeFile, ev.ts, ev.maybeOriginalEventType.map(_.toString), ev.isUninterruptedConversation)
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
            EventType.maybeFrom(event.maybeOriginalEventType),
            event.isUninterruptedConversation
          ))
        }
        case JsError(err) => None
      }
    }
  }

  implicit val invokeResultDataFormat = Json.format[InvokeResultData]

  def cacheInvokeResult(key: String, invokeResult: InvokeResult, expiration: Duration = Duration.Inf): Unit = {
    val data = InvokeResultData(invokeResult.getStatusCode, invokeResult.getLogResult, invokeResult.getPayload.array())
    set(key, Json.toJson(data), expiration)
  }

  def getInvokeResult(key: String): Option[InvokeResult] = {
    get[JsValue](key).flatMap { json =>
      json.validate[InvokeResultData] match {
        case JsSuccess(result, _) => {
          println(s"Found cached InvokeResult for $key")
          Some(
            new InvokeResult().
              withStatusCode(result.statusCode).
              withLogResult(result.logResult).
              withPayload(ByteBuffer.wrap(result.payload)))
        }
        case JsError(_) => None
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

  def clearDataTypeBotResult(key: DataTypeBotResultsCacheKey): Unit = {
    dataTypeBotResultsCache.remove(key)
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

  private def lastConversationIdKey(teamId: String, channelId: String): String = {
    s"team-$teamId-channel-$channelId-lastConversationId-v1"
  }

  def cacheLastConversationId(teamId: String, channelId: String, conversationId: String): Unit = {
    set(lastConversationIdKey(teamId, channelId), conversationId)
  }

  def clearLastConversationId(teamId: String, channelId: String): Unit = {
    remove(lastConversationIdKey(teamId, channelId))
  }

  def getLastConversationId(teamId: String, channelId: String): Option[String] = {
    get(lastConversationIdKey(teamId, channelId))
  }

}

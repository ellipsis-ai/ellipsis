package services.caching

import java.nio.ByteBuffer

import akka.actor.ActorSystem
import akka.http.caching.LfuCache
import akka.http.caching.scaladsl.{Cache, CachingSettings}
import com.amazonaws.services.lambda.model.InvokeResult
import javax.inject.{Inject, Provider, Singleton}
import json.Formatting._
import json.{ImmutableBehaviorGroupVersionData, SlackUserData}
import models.IDs
import models.accounts.slack.botprofile.SlackBotProfile
import models.behaviors.BotResult
import models.behaviors.behaviorparameter.ValidValue
import models.behaviors.defaultstorageitem.DefaultStorageItemService
import models.behaviors.events._
import play.api.Logger
import play.api.cache.SyncCacheApi
import play.api.libs.json._
import sangria.schema.Schema
import services.slack.SlackEventService
import services.slack.apiModels.{SlackUser, SlackUserProfile}

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
                                  isUninterruptedConversation: Boolean,
                                  isEphemeral: Boolean,
                                  maybeResponseUrl: Option[String],
                                  beQuiet: Boolean
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
  val defaultStorageSchemaExpiry: Duration = 10.seconds
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
        val eventData = SlackMessageEventData(
          ev.profile,
          ev.userSlackTeamId,
          ev.channel,
          ev.maybeThreadId,
          ev.user,
          ev.message,
          ev.maybeFile,
          ev.ts,
          ev.maybeOriginalEventType.map(_.toString),
          ev.isUninterruptedConversation,
          ev.isEphemeral,
          ev.maybeResponseUrl,
          ev.beQuiet
        )
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
            EventType.maybeFrom(event.maybeOriginalEventType),
            event.isUninterruptedConversation,
            event.isEphemeral,
            event.maybeResponseUrl,
            event.beQuiet
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
          Logger.info(s"Found cached InvokeResult for $key")
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

  def cacheSlackActionValue(value: String, expiration: Duration): String = {
    val key = s"slack-action-value-${IDs.next}"
    set(key, value, expiration)
    key
  }

  def getSlackActionValue(key: String): Option[String] = {
    get[String](key)
  }

  private val defaultStorageSchemaCache = LfuCache[DefaultStorageSchemaCacheKey, Schema[DefaultStorageItemService, Any]](cacheSettingsWithTimeToLive(defaultStorageSchemaExpiry))

  def getDefaultStorageSchema(key: DefaultStorageSchemaCacheKey, dataFn: DefaultStorageSchemaCacheKey => Future[Schema[DefaultStorageItemService, Any]]): Future[Schema[DefaultStorageItemService, Any]] = {
    defaultStorageSchemaCache.getOrLoad(key, dataFn)
  }

  private val dataTypeBotResultsCache = LfuCache[DataTypeBotResultsCacheKey, BotResult](cacheSettingsWithTimeToLive(dataTypeBotResultsExpiry))

  def getDataTypeBotResult(key: DataTypeBotResultsCacheKey, dataFn: DataTypeBotResultsCacheKey => Future[BotResult]): Future[BotResult] = {
    dataTypeBotResultsCache.getOrLoad(key, dataFn)
  }

  def clearDataTypeBotResult(key: DataTypeBotResultsCacheKey): Unit = {
    dataTypeBotResultsCache.remove(key)
  }

  private val slackUserDataCache: Cache[SlackUserDataCacheKey, Option[SlackUserData]] = LfuCache(cacheSettingsWithTimeToLive(slackApiCallExpiry))

  def getSlackUserData(key: SlackUserDataCacheKey, dataFn: SlackUserDataCacheKey => Future[Option[SlackUserData]]): Future[Option[SlackUserData]] = {
    slackUserDataCache.getOrLoad(key, dataFn)
  }

  private val slackUserDataByEmailCache: Cache[SlackUserDataByEmailCacheKey, Option[SlackUserData]] = LfuCache(cacheSettingsWithTimeToLive(slackApiCallExpiry))

  def getSlackUserDataByEmail(key: SlackUserDataByEmailCacheKey, dataFn: SlackUserDataByEmailCacheKey => Future[Option[SlackUserData]]): Future[Option[SlackUserData]] = {
    slackUserDataByEmailCache.getOrLoad(key, dataFn)
  }

  implicit val slackUserProfileJsonFormat = Json.format[SlackUserProfile]
  implicit val slackUserJsonFormat = Json.format[SlackUser]

  private def fallbackSlackUserCacheKey(slackUserId: String, slackTeamId: String): String = {
    s"fallbackCacheForSlackUserId-${slackUserId}-slackTeamId-${slackTeamId}-v1"
  }

  def cacheFallbackSlackUser(slackUserId: String, slackTeamId: String, slackUser: SlackUser) = {
    set(fallbackSlackUserCacheKey(slackUserId, slackTeamId), Json.toJson(slackUser))
  }

  def getFallbackSlackUser(slackUserId: String, slackTeamId: String): Option[SlackUser] = {
    val key = fallbackSlackUserCacheKey(slackUserId, slackTeamId)
    get[JsValue](key).flatMap { json =>
      json.validate[SlackUser] match {
        case JsSuccess(slackUser, _) => Some(slackUser)
        case JsError(_) => {
          remove(key)
          None
        }
      }
    }
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

  private def cacheKeyForMessageUserDataList(conversationId: String): String = {
    s"conversation-${conversationId}-messageUserDataList-v1"
  }

  def cacheMessageUserDataList(messageUserDataList: Seq[MessageUserData], conversationId: String): Unit = {
    val maybeExisting = getMessageUserDataList(conversationId)
    set(cacheKeyForMessageUserDataList(conversationId), Json.toJson(maybeExisting.getOrElse(Seq()) ++ messageUserDataList), Duration.Inf)
  }

  def getMessageUserDataList(conversationId: String): Option[Seq[MessageUserData]] = {
    get[JsValue](cacheKeyForMessageUserDataList(conversationId)).flatMap { json =>
      json.validate[Seq[MessageUserData]] match {
        case JsSuccess(data, _) => Some(data)
        case JsError(_) => None
      }
    }
  }

}

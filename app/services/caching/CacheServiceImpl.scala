package services.caching

import java.nio.ByteBuffer

import akka.Done
import akka.actor.ActorSystem
import akka.http.caching.LfuCache
import akka.http.caching.scaladsl.{Cache, CachingSettings}
import com.amazonaws.services.lambda.model.InvokeResult
import javax.inject.{Inject, Provider, Singleton}
import json.Formatting._
import json.{ImmutableBehaviorGroupVersionData, SlackUserData, UserData}
import models.IDs
import models.accounts.ms_teams.botprofile.MSTeamsBotProfile
import models.accounts.slack.botprofile.SlackBotProfile
import models.behaviors.BotResult
import models.behaviors.behaviorparameter.ValidValue
import models.behaviors.defaultstorageitem.DefaultStorageItemService
import models.behaviors.events._
import models.behaviors.events.slack.{SlackFile, SlackMessage, SlackMessageEvent}
import play.api.Logger
import play.api.cache.AsyncCacheApi
import play.api.libs.json._
import sangria.schema.Schema
import services.ms_teams.apiModels.{Application, MSAADUser}
import services.ms_teams.{ChannelWithTeam, MSTeamsApiService}
import services.slack.SlackEventService
import services.slack.apiModels.{SlackUser, SlackUserProfile}

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.ClassTag

case class SlackMessageEventData(
                                  profile: SlackBotProfile,
                                  channel: String,
                                  maybeThreadId: Option[String],
                                  user: String,
                                  message: SlackMessage,
                                  maybeFile: Option[SlackFile],
                                  ts: Option[String],
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
                                   cache: AsyncCacheApi,
                                   slackEventServiceProvider: Provider[SlackEventService],
                                   msTeamsApiServiceProvider: Provider[MSTeamsApiService],
                                   implicit val ec: ExecutionContext,
                                   implicit val actorSystem: ActorSystem
                                 ) extends CacheService {

  val MAX_KEY_LENGTH = 250

  def slackEventService = slackEventServiceProvider.get
  def msTeamsApiService = msTeamsApiServiceProvider.get

  def cacheSettingsWithTimeToLive(duration: Duration): CachingSettings = {
    val defaultCachingSettings = CachingSettings(actorSystem)
    val shortLivedLfuCacheSettings =
      defaultCachingSettings.lfuCacheSettings
        .withTimeToLive(duration)
        .withTimeToIdle(duration.div(2))
    defaultCachingSettings.withLfuCacheSettings(shortLivedLfuCacheSettings)
  }

  val slackApiCallExpiry: Duration = 10.seconds
  val msTeamsApiCallExpiry: Duration = 10.seconds
  val defaultStorageSchemaExpiry: Duration = 10.seconds
  val dataTypeBotResultsExpiry: Duration = 24.hours

  def set[T: ClassTag](key: String, value: T, expiration: Duration = Duration.Inf): Future[Unit] = {
    if (key.getBytes().length <= MAX_KEY_LENGTH) {
      // Even though the underlying cache set returns a scala future, the key validation exception
      // doesn't fail it; instead it throws an exception in the java library. Here we try to fix this.
      try {
        cache.set(key, value, expiration).map(_ => {})
      } catch {
        case e: Throwable => Future.failed(e)
      }
    } else {
      Future.successful({})
    }
  }

  def get[T : ClassTag](key: String): Future[Option[T]] = {
    if (key.getBytes().length <= MAX_KEY_LENGTH) {
      // Even though the underlying cache get returns a scala future, the key validation exception
      // doesn't fail it; instead it throws an exception in the java library. Here we try to fix this.
      try {
        cache.get[T](key)
      } catch {
        case e: Throwable => Future.failed(e)
      }
    } else {
      Future.successful(None)
    }
  }

  def hasKey(key: String): Future[Boolean] = {
    cache.get(key).map(_.isDefined)
  }

  def remove(key: String): Future[Done] = {
    cache.remove(key)
  }

  def cacheEvent(key: String, event: Event, expiration: Duration = Duration.Inf): Future[Unit] = {
    event match {
      case ev: SlackMessageEvent => {
        val eventData = SlackMessageEventData(
          ev.profile,
          ev.channel,
          ev.maybeThreadId,
          ev.eventContext.userIdForContext,
          ev.message,
          ev.maybeFile,
          ev.maybeTs,
          Some(ev.originalEventType.toString),
          ev.isUninterruptedConversation,
          ev.isEphemeral,
          ev.maybeResponseUrl,
          ev.beQuiet
        )
        set(key, Json.toJson(eventData), expiration)
      }
      case _ => Future.successful({})
    }
  }

  def getEvent(key: String): Future[Option[SlackMessageEvent]] = {
    get[JsValue](key).map { maybeValue =>
      maybeValue.flatMap { eventJson =>
        eventJson.validate[SlackMessageEventData] match {
          case JsSuccess(event, _) => {
            Some(SlackMessageEvent(
              SlackEventContext(
                event.profile,
                event.channel,
                event.maybeThreadId,
                event.user
              ),
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
          case JsError(_) => None
        }
      }
    }
  }

  implicit val invokeResultDataFormat = Json.format[InvokeResultData]

  def cacheInvokeResult(key: String, invokeResult: InvokeResult, expiration: Duration = Duration.Inf): Future[Unit] = {
    val data = InvokeResultData(invokeResult.getStatusCode, invokeResult.getLogResult, invokeResult.getPayload.array())
    set(key, Json.toJson(data), expiration)
  }

  def getInvokeResult(key: String): Future[Option[InvokeResult]] = {
    get[JsValue](key).map { maybeValue =>
      maybeValue.flatMap { json =>
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
  }

  def cacheValidValues(key: String, values: Seq[ValidValue], expiration: Duration = Duration.Inf): Future[Unit] = {
    set(key, Json.toJson(values), expiration)
  }

  def getValidValues(key: String): Future[Option[Seq[ValidValue]]] = {
    get[JsValue](key).map { maybeValue =>
      maybeValue.flatMap { json =>
        json.validate[Seq[ValidValue]] match {
          case JsSuccess(values, jsPath) => Some(values)
          case JsError(err) => None
        }
      }
    }
  }

  def cacheSlackActionValue(value: String, expiration: Duration): Future[String] = {
    val key = s"slack-action-value-${IDs.next}"
    set(key, value, expiration).map(_ => key)
  }

  def getSlackActionValue(key: String): Future[Option[String]] = {
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
  import services.slack.apiModels.Formatting._

  private def fallbackSlackUserCacheKey(slackUserId: String, slackTeamId: String): String = {
    s"fallbackCacheForSlackUserId-${slackUserId}-slackTeamId-${slackTeamId}-v1"
  }

  def cacheFallbackSlackUser(slackUserId: String, slackTeamId: String, slackUser: SlackUser): Future[Unit] = {
    set(fallbackSlackUserCacheKey(slackUserId, slackTeamId), Json.toJson(slackUser))
  }

  def getFallbackSlackUser(slackUserId: String, slackTeamId: String): Future[Option[SlackUser]] = {
    val key = fallbackSlackUserCacheKey(slackUserId, slackTeamId)
    get[JsValue](key).map { maybeValue =>
      maybeValue.flatMap { json =>
        json.validate[SlackUser] match {
          case JsSuccess(slackUser, _) => Some(slackUser)
          case JsError(_) => {
            remove(key)
            None
          }
        }
      }
    }
  }

  private val msTeamsApplicationDataCache: Cache[String, Option[Application]] = LfuCache(cacheSettingsWithTimeToLive(msTeamsApiCallExpiry))

  def getMSTeamsApplicationData(teamIdForContext: String, dataFn: String => Future[Option[Application]]): Future[Option[Application]] = {
    msTeamsApplicationDataCache.getOrLoad(teamIdForContext, dataFn)
  }

  private val msTeamsChannelDataCache: Cache[String, Map[String, ChannelWithTeam]] = LfuCache(cacheSettingsWithTimeToLive(msTeamsApiCallExpiry))

  def getMSTeamsChannelFor(profile: MSTeamsBotProfile, channelId: String): Future[Option[ChannelWithTeam]] = {
    for {
      channelMap <- msTeamsChannelDataCache.getOrLoad(profile.teamIdForContext, _ => msTeamsApiService.profileClientFor(profile).getChannelMap)
    } yield channelMap.get(channelId)
  }

  private val msTeamsUserCache: Cache[String, Option[MSAADUser]] = LfuCache(cacheSettingsWithTimeToLive(msTeamsApiCallExpiry))

  def getMSAADUser(key: String, dataFn: String => Future[Option[MSAADUser]]): Future[Option[MSAADUser]] = {
    msTeamsUserCache.getOrLoad(key, dataFn)
  }

  private def groupVersionDataKey(versionId: String): String = {
    s"ImmutableBehaviorGroupVersionData-v1-${versionId}"
  }

  def cacheBehaviorGroupVersionData(data: ImmutableBehaviorGroupVersionData): Future[Unit] = {
    set(groupVersionDataKey(data.id), Json.toJson(data))
  }

  def getBehaviorGroupVersionData(groupVersionId: String): Future[Option[ImmutableBehaviorGroupVersionData]] = {
    get[JsValue](groupVersionDataKey(groupVersionId)).map { maybeValue =>
      maybeValue.flatMap { json =>
        json.validate[ImmutableBehaviorGroupVersionData] match {
          case JsSuccess(data, _) => Some(data)
          case JsError(_) => None
        }
      }
    }
  }

  private def botNameKey(teamId: String): String = {
    s"team-$teamId-bot-name-v1"
  }

  def cacheBotName(name: String, teamId: String): Future[Unit] = {
    set(botNameKey(teamId), name, Duration.Inf)
  }

  def getBotName(teamId: String): Future[Option[String]] = {
    get[String](botNameKey(teamId))
  }

  private def lastConversationIdKey(teamId: String, channelId: String): String = {
    s"team-$teamId-channel-$channelId-lastConversationId-v1"
  }

  def cacheLastConversationId(teamId: String, channelId: String, conversationId: String): Future[Unit] = {
    set(lastConversationIdKey(teamId, channelId), conversationId).map(_ => {})
  }

  def clearLastConversationId(teamId: String, channelId: String): Future[Unit] = {
    remove(lastConversationIdKey(teamId, channelId)).map(_ => {})
  }

  def getLastConversationId(teamId: String, channelId: String): Future[Option[String]] = {
    get(lastConversationIdKey(teamId, channelId))
  }

  private def cacheKeyForMessageUserDataList(conversationId: String): String = {
    s"conversation-${conversationId}-messageUserDataList-v1"
  }

  def cacheMessageUserDataList(messageUserDataList: Seq[UserData], conversationId: String): Future[Unit] = {
    getMessageUserDataList(conversationId).flatMap { maybeExisting =>
      set(cacheKeyForMessageUserDataList(conversationId), Json.toJson(maybeExisting.getOrElse(Seq()) ++ messageUserDataList), Duration.Inf)
    }
  }

  def getMessageUserDataList(conversationId: String): Future[Option[Seq[UserData]]] = {
    get[JsValue](cacheKeyForMessageUserDataList(conversationId)).map { maybeValue =>
      maybeValue.flatMap { json =>
        json.validate[Seq[UserData]] match {
          case JsSuccess(data, _) => Some(data)
          case JsError(_) => None
        }
      }
    }
  }

  private def cacheKeyForSlackUserIsOnBotTeam(slackUserId: String, profile: SlackBotProfile, maybeEnterpriseId: Option[String]): String = {
    s"slackUserId-${slackUserId}-${maybeEnterpriseId.map(enterpriseId => s"fromEnterpriseGridId-${enterpriseId}-")}isOnSlackTeamId-${profile.slackTeamId}"
  }

  def cacheSlackUserIsValidForBotTeam(slackUserId: String, slackBotProfile: SlackBotProfile, maybeEnterpriseId: Option[String], userIsOnTeam: Boolean): Future[Unit] = {
    val duration = if (maybeEnterpriseId.isDefined) {
      10.seconds // On enterprise grid Slack, a user's team(s) can change at any time
    } else {
      Duration.Inf
    }
    set(cacheKeyForSlackUserIsOnBotTeam(slackUserId, slackBotProfile, maybeEnterpriseId), userIsOnTeam, duration)
  }

  def getSlackUserIsValidForBotTeam(slackUserId: String, slackBotProfile: SlackBotProfile, maybeEnterpriseId: Option[String]): Future[Option[Boolean]] = {
    get[Boolean](cacheKeyForSlackUserIsOnBotTeam(slackUserId, slackBotProfile, maybeEnterpriseId))
  }

  private val slackMessagePermalinkCache: Cache[SlackMessagePermalinkCacheKey, Option[String]] = LfuCache(cacheSettingsWithTimeToLive(slackApiCallExpiry))

  def getSlackPermalinkForMessage(key: SlackMessagePermalinkCacheKey, dataFn: SlackMessagePermalinkCacheKey => Future[Option[String]]): Future[Option[String]] = {
    slackMessagePermalinkCache.getOrLoad(key, dataFn)
  }
}

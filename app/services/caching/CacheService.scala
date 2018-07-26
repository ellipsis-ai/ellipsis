package services.caching

import com.amazonaws.services.lambda.model.InvokeResult
import json.{ImmutableBehaviorGroupVersionData, SlackUserData}
import models.behaviors.BotResult
import models.behaviors.behaviorparameter.ValidValue
import models.behaviors.defaultstorageitem.DefaultStorageItemService
import models.behaviors.events.{Event, SlackMessageEvent}
import sangria.schema.Schema

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.reflect.ClassTag

trait CacheService {

  def set[T: ClassTag](key: String, value: T, expiration: Duration = Duration.Inf): Unit

  def get[T : ClassTag](key: String): Option[T]

  def hasKey(key: String): Boolean

  def remove(key: String)

  def cacheEvent(key: String, event: Event, expiration: Duration = Duration.Inf): Unit

  def getEvent(key: String): Option[SlackMessageEvent]

  def cacheInvokeResult(key: String, invokeResult: InvokeResult, expiration: Duration = Duration.Inf): Unit

  def getInvokeResult(key: String): Option[InvokeResult]

  def cacheValidValues(key: String, values: Seq[ValidValue], expiration: Duration = Duration.Inf): Unit

  def getValidValues(key: String): Option[Seq[ValidValue]]

  def cacheSlackActionValue(value: String, expiration: Duration = Duration.Inf): String

  def getSlackActionValue(key: String): Option[String]

  def getDefaultStorageSchema(key: DefaultStorageSchemaCacheKey, dataFn: DefaultStorageSchemaCacheKey => Future[Schema[DefaultStorageItemService, Any]]): Future[Schema[DefaultStorageItemService, Any]]

  def getDataTypeBotResult(key: DataTypeBotResultsCacheKey, dataFn: DataTypeBotResultsCacheKey => Future[BotResult]): Future[BotResult]

  def clearDataTypeBotResult(key: DataTypeBotResultsCacheKey): Unit

  def getSlackUserData(key: SlackUserDataCacheKey, dataFn: SlackUserDataCacheKey => Future[Option[SlackUserData]]): Future[Option[SlackUserData]]

  def getSlackUserDataByEmail(
                               key: SlackUserDataByEmailCacheKey,
                               dataFn: SlackUserDataByEmailCacheKey => Future[Option[SlackUserData]]
                             ): Future[Option[SlackUserData]]

  def cacheBehaviorGroupVersionData(data: ImmutableBehaviorGroupVersionData): Unit

  def getBehaviorGroupVersionData(groupVersionId: String): Option[ImmutableBehaviorGroupVersionData]

  def cacheBotName(name: String, teamId: String): Unit

  def getBotName(teamId: String): Option[String]

  def cacheLastConversationId(teamId: String, channelId: String, conversationId: String): Unit

  def clearLastConversationId(teamId: String, channelId: String): Unit

  def getLastConversationId(teamId: String, channelId: String): Option[String]

  def eventHasLastConversationId(event: Event, conversationId: String): Boolean = {
    event.maybeChannel.exists { channel =>
      getLastConversationId(event.teamId, channel).contains(conversationId)
    }
  }

  def updateLastConversationIdFor(event: Event, conversationId: String): Unit = {
    event.maybeChannel.foreach { channel =>
      cacheLastConversationId(event.teamId, channel, conversationId)
    }
  }
}

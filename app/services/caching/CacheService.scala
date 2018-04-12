package services.caching

import json.{ImmutableBehaviorGroupVersionData, SlackUserData}
import models.behaviors.BotResult
import models.behaviors.behaviorparameter.ValidValue
import models.behaviors.events.{Event, SlackMessageEvent}
import slack.models.{Channel, Group, Im}

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

  def cacheValidValues(key: String, values: Seq[ValidValue], expiration: Duration = Duration.Inf): Unit

  def getValidValues(key: String): Option[Seq[ValidValue]]

  def getDataTypeBotResult(key: DataTypeBotResultsCacheKey, dataFn: DataTypeBotResultsCacheKey => Future[BotResult]): Future[BotResult]

  def getSlackChannelInfo(key: SlackChannelDataCacheKey, dataFn: SlackChannelDataCacheKey => Future[Option[Channel]]): Future[Option[Channel]]

  def getSlackGroupInfo(key: SlackGroupDataCacheKey, dataFn: SlackGroupDataCacheKey => Future[Option[Group]]): Future[Option[Group]]

  def getSlackChannels(teamId: String, dataFn: String => Future[Seq[Channel]]): Future[Seq[Channel]]

  def getSlackGroups(teamId: String, dataFn: String => Future[Seq[Group]]): Future[Seq[Group]]

  def getSlackIMs(teamId: String, dataFn: String => Future[Seq[Im]]): Future[Seq[Im]]

  def getSlackUserData(key: SlackUserDataCacheKey, dataFn: SlackUserDataCacheKey => Future[Option[SlackUserData]]): Future[Option[SlackUserData]]

  def cacheBehaviorGroupVersionData(data: ImmutableBehaviorGroupVersionData): Unit

  def getBehaviorGroupVersionData(groupVersionId: String): Option[ImmutableBehaviorGroupVersionData]

  def cacheBotName(name: String, teamId: String): Unit

  def getBotName(teamId: String): Option[String]

  def cacheLastConversationId(teamId: String, channelId: String, conversationId: String): Unit

  def clearLastConversationId(teamId: String, channelId: String): Unit

  def getLastConversationId(teamId: String, channelId: String): Option[String]

  def isLastConversationIdFor(event: Event, conversationId: String): Boolean = {
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

package mocks

import json.{ImmutableBehaviorGroupVersionData, SlackUserData}
import models.behaviors.BotResult
import models.behaviors.behaviorparameter.DataTypeResultBody
import models.behaviors.events.{Event, SlackMessageEvent}
import org.scalatest.mock.MockitoSugar
import services.caching._
import slack.models.{Channel, Group, Im}

import scala.concurrent.Future
import scala.concurrent.duration.Duration
import scala.reflect.ClassTag

class MockCacheService extends CacheService with MockitoSugar {

  def set[T: ClassTag](key: String, value: T, expiration: Duration = Duration.Inf): Unit = {}

  def get[T : ClassTag](key: String): Option[T] = None

  def hasKey(key: String): Boolean = false

  def remove(key: String) = {}

  def cacheEvent(key: String, event: Event, expiration: Duration = Duration.Inf): Unit = {}

  def getEvent(key: String): Option[SlackMessageEvent] = None

  def cacheDataTypeResultBody(key: String, resultBody: DataTypeResultBody, expiration: Duration = Duration.Inf): Unit = {}

  def getDataTypeResultBody(key: String): Option[DataTypeResultBody] = None

  def getDataTypeBotResult(key: DataTypeBotResultsCacheKey, dataFn: DataTypeBotResultsCacheKey => Future[BotResult]): Future[BotResult] = dataFn(key)

  def getSlackChannelInfo(
                           key: SlackChannelDataCacheKey,
                           dataFn: SlackChannelDataCacheKey => Future[Option[Channel]]
                         ): Future[Option[Channel]] = dataFn(key)

  def getSlackGroupInfo(
                         key: SlackGroupDataCacheKey,
                         dataFn: SlackGroupDataCacheKey => Future[Option[Group]]
                       ): Future[Option[Group]] = dataFn(key)

  def getSlackChannels(
                        teamId: String,
                        dataFn: String => Future[Seq[Channel]]
                      ): Future[Seq[Channel]] = dataFn(teamId)

  def getSlackGroups(
                      teamId: String,
                      dataFn: String => Future[Seq[Group]]
                    ): Future[Seq[Group]] = dataFn(teamId)

  def getSlackIMs(teamId: String, dataFn: String => Future[Seq[Im]]): Future[Seq[Im]] = dataFn(teamId)

  def getSlackUserData(
                        key: SlackUserDataCacheKey,
                        dataFn: SlackUserDataCacheKey => Future[Option[SlackUserData]]
                      ): Future[Option[SlackUserData]] = dataFn(key)

  def cacheBehaviorGroupVersionData(data: ImmutableBehaviorGroupVersionData): Unit = {}

  def getBehaviorGroupVersionData(groupVersionId: String): Option[ImmutableBehaviorGroupVersionData] = None

  def cacheBotName(name: String, teamId: String): Unit = {}

  def getBotName(teamId: String): Option[String] = Some("MockBot")
}

package mocks

import json.{ImmutableBehaviorGroupVersionData, SlackUserData}
import models.behaviors.behaviorparameter.ValidValue
import models.behaviors.events.{Event, SlackMessageEvent}
import org.scalatest.mock.MockitoSugar
import services.caching.{CacheService, SlackChannelDataCacheKey, SlackGroupDataCacheKey, SlackUserDataCacheKey}
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

  def cacheValidValues(key: String, values: Seq[ValidValue], expiration: Duration = Duration.Inf): Unit = {}

  def getValidValues(key: String): Option[Seq[ValidValue]] = None

  def getSlackChannelInfo(key: SlackChannelDataCacheKey, dataFn: SlackChannelDataCacheKey => Future[Option[Channel]]): Future[Option[Channel]] = Future.successful(None)

  def getSlackGroupInfo(key: SlackGroupDataCacheKey, dataFn: SlackGroupDataCacheKey => Future[Option[Group]]): Future[Option[Group]] = Future.successful(None)

  def getSlackChannels(teamId: String, dataFn: String => Future[Seq[Channel]]): Future[Seq[Channel]] = Future.successful(Seq())

  def getSlackGroups(teamId: String, dataFn: String => Future[Seq[Group]]): Future[Seq[Group]] = Future.successful(Seq())

  def getSlackIMs(teamId: String, dataFn: String => Future[Seq[Im]]): Future[Seq[Im]] = Future.successful(Seq())

  def getSlackUserData(key: SlackUserDataCacheKey, dataFn: SlackUserDataCacheKey => Future[Option[SlackUserData]]): Future[Option[SlackUserData]] = Future.successful(None)

  def cacheBehaviorGroupVersionData(data: ImmutableBehaviorGroupVersionData): Unit = {}

  def getBehaviorGroupVersionData(groupVersionId: String): Option[ImmutableBehaviorGroupVersionData] = None

}

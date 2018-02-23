package mocks

import json.{ImmutableBehaviorGroupVersionData, SlackUserData}
import models.behaviors.behaviorparameter.ValidValue
import models.behaviors.events.{Event, SlackMessageEvent}
import org.scalatest.mock.MockitoSugar
import services.{CacheService, SlackUserDataCacheKey}
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

  def cacheSlackChannelInfo(channel: String, teamId: String, data: Channel): Unit = {}

  def getSlackChannelInfo(channel: String, teamId: String): Option[Channel] = None

  def cacheSlackGroupInfo(group: String, teamId: String, data: Group): Unit = {}

  def getSlackGroupInfo(group: String, teamId: String): Option[Group] = None

  def getSlackChannels(teamId: String, dataFn: String => Future[Seq[Channel]]): Future[Seq[Channel]] = Future.successful(Seq())

  def cacheSlackGroups(data: Seq[Group], teamId: String): Unit = {}

  def getSlackGroups(teamId: String): Option[Seq[Group]] = None

  def cacheSlackIMs(data: Seq[Im], teamId: String): Unit = {}

  def getSlackIMs(teamId: String): Option[Seq[Im]] = None

  def cacheSlackUserData(userData: SlackUserData): Unit = {}

  def getSlackUserData(key: SlackUserDataCacheKey, dataFn: SlackUserDataCacheKey => Future[Option[SlackUserData]]): Future[Option[SlackUserData]] = Future.successful(None)

  def cacheBehaviorGroupVersionData(data: ImmutableBehaviorGroupVersionData): Unit = {}

  def getBehaviorGroupVersionData(groupVersionId: String): Option[ImmutableBehaviorGroupVersionData] = None

}

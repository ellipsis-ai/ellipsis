package services

import json.{ImmutableBehaviorGroupVersionData, SlackUserData}
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

  def cacheSlackChannelInfo(channel: String, teamId: String, data: Channel): Unit

  def getSlackChannelInfo(channel: String, teamId: String): Option[Channel]

  def cacheSlackGroupInfo(group: String, teamId: String, data: Group): Unit

  def getSlackGroupInfo(group: String, teamId: String): Option[Group]

  def getSlackChannels(teamId: String, dataFn: String => Future[Seq[Channel]]): Future[Seq[Channel]]

  def cacheSlackGroups(data: Seq[Group], teamId: String): Unit

  def getSlackGroups(teamId: String): Option[Seq[Group]]

  def cacheSlackIMs(data: Seq[Im], teamId: String): Unit

  def getSlackIMs(teamId: String): Option[Seq[Im]]

  def getSlackUserData(key: SlackUserDataCacheKey, dataFn: SlackUserDataCacheKey => Future[Option[SlackUserData]]): Future[Option[SlackUserData]]

  def cacheBehaviorGroupVersionData(data: ImmutableBehaviorGroupVersionData): Unit

  def getBehaviorGroupVersionData(groupVersionId: String): Option[ImmutableBehaviorGroupVersionData]
}

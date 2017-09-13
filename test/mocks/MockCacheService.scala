package mocks

import json.BehaviorGroupData
import models.accounts.slack.SlackUserInfo
import models.behaviors.behaviorparameter.ValidValue
import models.behaviors.events.{Event, SlackMessageEvent}
import org.scalatest.mock.MockitoSugar
import services.CacheService
import slack.models.{Channel, Group, Im}

import scala.concurrent.duration.Duration
import scala.reflect.ClassTag

class MockCacheService extends CacheService with MockitoSugar {

  def set[T: ClassTag](key: String, value: T, expiration: Duration = Duration.Inf): Unit = {}

  def get[T : ClassTag](key: String): Option[T] = None

  def remove(key: String) = {}

  def cacheEvent(key: String, event: Event, expiration: Duration = Duration.Inf): Unit = {}

  def getEvent(key: String): Option[SlackMessageEvent] = None

  def cacheValidValues(key: String, values: Seq[ValidValue], expiration: Duration = Duration.Inf): Unit = {}

  def getValidValues(key: String): Option[Seq[ValidValue]] = None

  def cacheBehaviorGroupData(key: String, data: Seq[BehaviorGroupData], expiration: Duration = Duration.Inf): Unit = {}

  def getBehaviorGroupData(key: String): Option[Seq[BehaviorGroupData]] = None

  def cacheSlackUserList(key: String, data: Seq[SlackUserInfo]): Unit = {}

  def getSlackUserList(key: String): Option[Seq[SlackUserInfo]] = None

  def cacheSlackChannelInfo(channel: String, teamId: String, data: Channel): Unit = {}

  def getSlackChannelInfo(channel: String, teamId: String): Option[Channel] = None

  def cacheSlackGroupInfo(group: String, teamId: String, data: Group): Unit = {}

  def getSlackGroupInfo(group: String, teamId: String): Option[Group] = None

  def cacheSlackIMs(data: Seq[Im], teamId: String): Unit = {}

  def getSlackIMs(teamId: String): Option[Seq[Im]] = None

  def cacheBotUsername(userId: String, username: String): Unit = {}

  def getBotUsername(userId: String): Option[String] = None

}

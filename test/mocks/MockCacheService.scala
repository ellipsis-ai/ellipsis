package mocks

import com.amazonaws.services.lambda.model.InvokeResult
import json.{ImmutableBehaviorGroupVersionData, SlackUserData, UserData}
import models.IDs
import models.accounts.ms_teams.botprofile.MSTeamsBotProfile
import models.accounts.slack.botprofile.SlackBotProfile
import models.behaviors.BotResult
import models.behaviors.behaviorparameter.ValidValue
import models.behaviors.defaultstorageitem.DefaultStorageItemService
import models.behaviors.events.Event
import models.behaviors.events.slack.SlackMessageEvent
import org.scalatest.mock.MockitoSugar
import sangria.schema.Schema
import services.caching._
import services.ms_teams.ChannelWithTeam
import services.ms_teams.apiModels.{Application, MSTeamsUser}
import services.slack.apiModels.SlackUser

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

  def cacheInvokeResult(key: String, invokeResult: InvokeResult, expiration: Duration = Duration.Inf): Unit = {}

  def getInvokeResult(key: String): Option[InvokeResult] = None

  def cacheValidValues(key: String, values: Seq[ValidValue], expiration: Duration = Duration.Inf): Unit = {}

  def getValidValues(key: String): Option[Seq[ValidValue]] = None

  def cacheSlackActionValue(value: String, expiration: Duration = Duration.Inf): String = IDs.next

  def getSlackActionValue(key: String): Option[String] = None

  def getDefaultStorageSchema(key: DefaultStorageSchemaCacheKey, dataFn: DefaultStorageSchemaCacheKey => Future[Schema[DefaultStorageItemService, Any]]): Future[Schema[DefaultStorageItemService, Any]] = dataFn(key)

  def getDataTypeBotResult(key: DataTypeBotResultsCacheKey, dataFn: DataTypeBotResultsCacheKey => Future[BotResult]): Future[BotResult] = dataFn(key)

  def clearDataTypeBotResult(key: DataTypeBotResultsCacheKey): Unit = {}

  def getSlackUserData(
                        key: SlackUserDataCacheKey,
                        dataFn: SlackUserDataCacheKey => Future[Option[SlackUserData]]
                      ): Future[Option[SlackUserData]] = dataFn(key)

  def getSlackUserDataByEmail(
                               key: SlackUserDataByEmailCacheKey,
                               dataFn: SlackUserDataByEmailCacheKey => Future[Option[SlackUserData]]
                             ): Future[Option[SlackUserData]] = dataFn(key)

  def cacheFallbackSlackUser(slackUserId: String, slackTeamId: String, slackUser: SlackUser): Unit = {}

  def getFallbackSlackUser(slackUserId: String, slackTeamId: String): Option[SlackUser] = None

  def getMSTeamsApplicationData(teamIdForContext: String, dataFn: String => Future[Option[Application]]): Future[Option[Application]] = dataFn(teamIdForContext)

  def getMSTeamsChannelFor(profile: MSTeamsBotProfile, channelId: String): Future[Option[ChannelWithTeam]] = Future.successful(None)

  def getMSTeamsUser(key: String, dataFn: String => Future[Option[MSTeamsUser]]): Future[Option[MSTeamsUser]] = Future.successful(None)

  def cacheBehaviorGroupVersionData(data: ImmutableBehaviorGroupVersionData): Unit = {}

  def getBehaviorGroupVersionData(groupVersionId: String): Option[ImmutableBehaviorGroupVersionData] = None

  def cacheBotName(name: String, teamId: String): Unit = {}

  def getBotName(teamId: String): Option[String] = Some("MockBot")

  def cacheLastConversationId(teamId: String, channelId: String, conversationId: String): Unit = {}

  def clearLastConversationId(teamId: String, channelId: String): Unit = {}

  def getLastConversationId(teamId: String, channelId: String): Option[String] = None

  def cacheMessageUserDataList(messageUserDataList: Seq[UserData], conversationId: String): Unit = {}

  def getMessageUserDataList(conversationId: String): Option[Seq[UserData]] = None

  def cacheSlackUserIsValidForBotTeam(slackUserId: String, slackBotProfile: SlackBotProfile, maybeEnterpriseId: Option[String], userIsOnTeam: Boolean): Unit = {}

  def getSlackUserIsValidForBotTeam(slackUserId: String, slackBotProfile: SlackBotProfile, maybeEnterpriseId: Option[String]): Option[Boolean] = None

  def getSlackPermalinkForMessage(key: SlackMessagePermalinkCacheKey, dataFn: SlackMessagePermalinkCacheKey => Future[Option[String]]): Future[Option[String]] = dataFn(key)
}

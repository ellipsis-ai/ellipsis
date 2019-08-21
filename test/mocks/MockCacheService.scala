package mocks

import akka.Done
import com.amazonaws.services.lambda.model.InvokeResult
import json.{ImmutableBehaviorGroupVersionData, SlackUserData, UserData}
import models.IDs
import models.accounts.ms_teams.botprofile.MSTeamsBotProfile
import models.accounts.slack.botprofile.SlackBotProfile
import models.behaviors.{BotResult, SuccessResult}
import models.behaviors.behaviorparameter.ValidValue
import models.behaviors.defaultstorageitem.DefaultStorageItemService
import models.behaviors.events.Event
import models.behaviors.events.slack.SlackMessageEvent
import org.scalatest.mockito.MockitoSugar
import play.api.libs.json.Reads
import sangria.schema.Schema
import services.caching._
import services.ms_teams.ChannelWithTeam
import services.ms_teams.apiModels.{Application, MSAADUser}
import services.slack.apiModels.SlackUser
import slick.dbio.DBIO

import scala.concurrent.Future
import scala.concurrent.duration.Duration
import scala.reflect.ClassTag

class MockCacheService extends CacheService with MockitoSugar {

  def set[T: ClassTag](key: String, value: T, expiration: Duration = Duration.Inf): Future[Unit] = Future.successful({})

  def get[T : ClassTag](key: String): Future[Option[T]] = Future.successful(None)

  def getJsonReadable[T](key: String)(implicit tjs: Reads[T]): Future[Option[T]] = Future.successful(None)

  def hasKey(key: String): Future[Boolean] = Future.successful(false)

  def remove(key: String): Future[Done] = Future.successful(Done)

  def cacheSuccessResult(key: String): Future[Unit] = Future.successful({})

  def getSuccessResultForCopilot(key: String): Future[Option[SuccessResult]] = Future.successful(None)

  def cacheEvent(key: String, event: Event, expiration: Duration = Duration.Inf): Future[Unit] = Future.successful({})

  def getEvent(key: String): Future[Option[SlackMessageEvent]] = Future.successful(None)

  def cacheInvokeResult(key: String, invokeResult: InvokeResult, expiration: Duration = Duration.Inf): Future[Unit] = Future.successful({})

  def getInvokeResult(key: String): Future[Option[InvokeResult]] = Future.successful(None)

  def cacheValidValues(key: String, values: Seq[ValidValue], expiration: Duration = Duration.Inf): Future[Unit] = Future.successful({})

  def getValidValues(key: String): Future[Option[Seq[ValidValue]]] = Future.successful(None)

  def cacheSlackActionValue(value: String, expiration: Duration = Duration.Inf): Future[String] = Future.successful(IDs.next)

  def getSlackActionValue(key: String): Future[Option[String]] = Future.successful(None)

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

  def cacheFallbackSlackUser(slackUserId: String, slackTeamId: String, slackUser: SlackUser): Future[Unit] = Future.successful({})

  def getFallbackSlackUser(slackUserId: String, slackTeamId: String): Future[Option[SlackUser]] = Future.successful(None)

  def getMSTeamsApplicationData(teamIdForContext: String, dataFn: String => Future[Option[Application]]): Future[Option[Application]] = dataFn(teamIdForContext)

  def getMSTeamsChannelFor(profile: MSTeamsBotProfile, channelId: String): Future[Option[ChannelWithTeam]] = Future.successful(None)

  def getMSAADUserAction(key: String, fetch: DBIO[Option[MSAADUser]]): DBIO[Option[MSAADUser]] = DBIO.successful(None)

  def cacheBehaviorGroupVersionData(data: ImmutableBehaviorGroupVersionData): Future[Unit] = Future.successful({})

  def getBehaviorGroupVersionDataAction(groupVersionId: String): DBIO[Option[ImmutableBehaviorGroupVersionData]] = DBIO.successful(None)

  def cacheBotNameAction(name: String, teamId: String): DBIO[Unit] = DBIO.successful({})
  def cacheBotName(name: String, teamId: String): Future[Unit] = Future.successful({})

  def getBotNameAction(teamId: String): DBIO[Option[String]] = DBIO.successful(Some("MockBot"))
  def getBotName(teamId: String): Future[Option[String]] = Future.successful(Some("MockBot"))

  def cacheLastConversationId(teamId: String, channelId: String, conversationId: String): Future[Unit] = Future.successful({})

  def clearLastConversationId(teamId: String, channelId: String): Future[Unit] = Future.successful({})

  def getLastConversationId(teamId: String, channelId: String): Future[Option[String]] = Future.successful(None)

  def cacheMessageUserDataList(messageUserDataList: Seq[UserData], conversationId: String): Future[Unit] = Future.successful({})

  def getMessageUserDataList(conversationId: String): Future[Option[Seq[UserData]]] = Future.successful(None)

  def cacheSlackUserIsValidForBotTeam(slackUserId: String, slackBotProfile: SlackBotProfile, maybeEnterpriseId: Option[String], userIsOnTeam: Boolean): Future[Unit] = Future.successful({})

  def getSlackUserIsValidForBotTeam(slackUserId: String, slackBotProfile: SlackBotProfile, maybeEnterpriseId: Option[String]): Future[Option[Boolean]] = Future.successful(None)

  def getSlackPermalinkForMessage(key: SlackMessagePermalinkCacheKey, dataFn: SlackMessagePermalinkCacheKey => Future[Option[String]]): Future[Option[String]] = dataFn(key)
}

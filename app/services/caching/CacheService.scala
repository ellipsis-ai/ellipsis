package services.caching

import akka.Done
import com.amazonaws.services.lambda.model.InvokeResult
import json.{ImmutableBehaviorGroupVersionData, SlackUserData, UserData}
import models.accounts.ms_teams.botprofile.MSTeamsBotProfile
import models.accounts.slack.botprofile.SlackBotProfile
import models.behaviors.BotResult
import models.behaviors.behaviorparameter.ValidValue
import models.behaviors.defaultstorageitem.DefaultStorageItemService
import models.behaviors.events.Event
import models.behaviors.events.slack.SlackMessageEvent
import sangria.schema.Schema
import services.ms_teams.ChannelWithTeam
import services.ms_teams.apiModels.{Application, MSAADUser}
import services.slack.apiModels.SlackUser

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._
import scala.reflect.ClassTag

trait CacheService {

  def set[T: ClassTag](key: String, value: T, expiration: Duration = Duration.Inf): Future[Unit]

  def get[T : ClassTag](key: String): Future[Option[T]]

  def hasKey(key: String): Future[Boolean]

  def remove(key: String): Future[Done]

  def cacheEvent(key: String, event: Event, expiration: Duration = Duration.Inf): Future[Unit]

  def getEvent(key: String): Future[Option[SlackMessageEvent]]

  def cacheInvokeResult(key: String, invokeResult: InvokeResult, expiration: Duration = Duration.Inf): Future[Unit]

  def getInvokeResult(key: String): Future[Option[InvokeResult]]

  def cacheValidValues(key: String, values: Seq[ValidValue], expiration: Duration = Duration.Inf): Future[Unit]

  def getValidValues(key: String): Future[Option[Seq[ValidValue]]]

  def cacheSlackActionValue(value: String, expiration: Duration = Duration.Inf): Future[String]

  def getSlackActionValue(key: String): Future[Option[String]]

  def getDefaultStorageSchema(key: DefaultStorageSchemaCacheKey, dataFn: DefaultStorageSchemaCacheKey => Future[Schema[DefaultStorageItemService, Any]]): Future[Schema[DefaultStorageItemService, Any]]

  def getDataTypeBotResult(key: DataTypeBotResultsCacheKey, dataFn: DataTypeBotResultsCacheKey => Future[BotResult]): Future[BotResult]

  def clearDataTypeBotResult(key: DataTypeBotResultsCacheKey): Unit

  def getSlackUserData(key: SlackUserDataCacheKey, dataFn: SlackUserDataCacheKey => Future[Option[SlackUserData]]): Future[Option[SlackUserData]]

  def getSlackUserDataByEmail(
                               key: SlackUserDataByEmailCacheKey,
                               dataFn: SlackUserDataByEmailCacheKey => Future[Option[SlackUserData]]
                             ): Future[Option[SlackUserData]]

  def cacheFallbackSlackUser(slackUserId: String, slackTeamId: String, slackUser: SlackUser): Future[Unit]

  def getFallbackSlackUser(slackUserId: String, slackTeamId: String): Future[Option[SlackUser]]

  def getMSTeamsApplicationData(teamIdForContext: String, dataFn: String => Future[Option[Application]]): Future[Option[Application]]

  def getMSTeamsChannelFor(profile: MSTeamsBotProfile, channelId: String): Future[Option[ChannelWithTeam]]

  def getMSAADUser(key: String, dataFn: String => Future[Option[MSAADUser]]): Future[Option[MSAADUser]]

  def cacheBehaviorGroupVersionData(data: ImmutableBehaviorGroupVersionData): Future[Unit]

  def getBehaviorGroupVersionData(groupVersionId: String): Future[Option[ImmutableBehaviorGroupVersionData]]

  def cacheBotName(name: String, teamId: String): Future[Unit]

  def getBotName(teamId: String): Future[Option[String]]

  def cacheLastConversationId(teamId: String, channelId: String, conversationId: String): Future[Unit]

  def clearLastConversationId(teamId: String, channelId: String): Future[Unit]

  def getLastConversationId(teamId: String, channelId: String): Future[Option[String]]

  def eventHasLastConversationId(event: Event, conversationId: String)(implicit ec: ExecutionContext): Future[Boolean] = {
    event.maybeChannel.map { channel =>
      getLastConversationId(event.ellipsisTeamId, channel).map(_.contains(conversationId))
    }.getOrElse(Future.successful(false))
  }

  def updateLastConversationIdFor(event: Event, conversationId: String): Future[Unit] = {
    event.maybeChannel.map { channel =>
      cacheLastConversationId(event.ellipsisTeamId, channel, conversationId)
    }.getOrElse(Future.successful({}))
  }

  def cacheMessageUserDataList(messageUserDataList: Seq[UserData], conversationId: String): Future[Unit]

  def getMessageUserDataList(conversationId: String): Future[Option[Seq[UserData]]]

  def cacheSlackUserIsValidForBotTeam(slackUserId: String, slackBotProfile: SlackBotProfile, maybeEnterpriseId: Option[String], userIsOnTeam: Boolean): Future[Unit]

  def getSlackUserIsValidForBotTeam(slackUserId: String, slackBotProfile: SlackBotProfile, maybeEnterpriseId: Option[String]): Future[Option[Boolean]]

  def getSlackPermalinkForMessage(key: SlackMessagePermalinkCacheKey, dataFn: SlackMessagePermalinkCacheKey => Future[Option[String]]): Future[Option[String]]
}

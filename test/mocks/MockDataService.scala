package mocks

import javax.inject.Singleton

import models.accounts.linkedaccount.LinkedAccountService
import models.accounts.linkedoauth2token.LinkedOAuth2TokenService
import models.accounts.linkedsimpletoken.LinkedSimpleTokenService
import models.accounts.logintoken.LoginTokenService
import models.accounts.oauth2api.OAuth2ApiService
import models.accounts.oauth2application.OAuth2ApplicationService
import models.accounts.slack.profile.SlackProfileService
import models.accounts.oauth2token.OAuth2TokenService
import models.accounts.simpletokenapi.SimpleTokenApiService
import models.accounts.slack.botprofile.SlackBotProfileService
import models.accounts.user.UserService
import models.apitoken.APITokenService
import models.behaviors.BehaviorResponseService
import models.behaviors.behavior.BehaviorService
import models.behaviors.behaviorgroup.BehaviorGroupService
import models.behaviors.behaviorgroupversion.BehaviorGroupVersionService
import models.behaviors.behaviorparameter.BehaviorParameterService
import models.behaviors.behaviorversion.BehaviorVersionService
import models.behaviors.config.awsconfig.AWSConfigService
import models.behaviors.config.requiredoauth2apiconfig.RequiredOAuth2ApiConfigService
import models.behaviors.config.requiredsimpletokenapi.RequiredSimpleTokenApiService
import models.behaviors.conversations.collectedparametervalue.CollectedParameterValueService
import models.behaviors.conversations.conversation.ConversationService
import models.behaviors.datatypeconfig.DataTypeConfigService
import models.behaviors.datatypefield.DataTypeFieldService
import models.behaviors.defaultstorageitem.DefaultStorageItemService
import models.behaviors.input.InputService
import models.behaviors.invocationlogentry.InvocationLogEntryService
import models.behaviors.scheduling.scheduledmessage.ScheduledMessageService
import models.behaviors.triggers.messagetrigger.MessageTriggerService
import models.environmentvariable.{TeamEnvironmentVariableService, UserEnvironmentVariableService}
import models.behaviors.invocationtoken.InvocationTokenService
import models.behaviors.library.LibraryVersionService
import models.behaviors.nodemoduleversion.NodeModuleVersionService
import models.behaviors.savedanswer.SavedAnswerService
import models.behaviors.scheduling.recurrence.RecurrenceService
import models.behaviors.scheduling.scheduledbehavior.ScheduledBehaviorService
import models.team.TeamService
import org.scalatest.mock.MockitoSugar
import slick.dbio.DBIO
import services.DataService

import scala.concurrent.Future

@Singleton
class MockDataService extends DataService with MockitoSugar {

  val users = mock[UserService]
  val loginTokens = mock[LoginTokenService]
  val linkedAccounts = mock[LinkedAccountService]
  val teams = mock[TeamService]
  val apiTokens = mock[APITokenService]
  val teamEnvironmentVariables = mock[TeamEnvironmentVariableService]
  val userEnvironmentVariables = mock[UserEnvironmentVariableService]
  val invocationTokens = mock[InvocationTokenService]
  val linkedOAuth2Tokens = mock[LinkedOAuth2TokenService]
  val linkedSimpleTokens = mock[LinkedSimpleTokenService]
  val oauth2Apis = mock[OAuth2ApiService]
  val oauth2Applications = mock[OAuth2ApplicationService]
  val simpleTokenApis = mock[SimpleTokenApiService]
  val slackProfiles = mock[SlackProfileService]
  val slackBotProfiles = mock[SlackBotProfileService]
  val oauth2Tokens = mock[OAuth2TokenService]
  val behaviorGroups = mock[BehaviorGroupService]
  val behaviorGroupVersions = mock[BehaviorGroupVersionService]
  val behaviors = mock[BehaviorService]
  val behaviorVersions = mock[BehaviorVersionService]
  val dataTypeConfigs = mock[DataTypeConfigService]
  val dataTypeFields = mock[DataTypeFieldService]
  val defaultStorageItems = mock[DefaultStorageItemService]
  val behaviorParameters = mock[BehaviorParameterService]
  val inputs = mock[InputService]
  val libraries = mock[LibraryVersionService]
  val nodeModuleVersions = mock[NodeModuleVersionService]
  val savedAnswers = mock[SavedAnswerService]
  val messageTriggers = mock[MessageTriggerService]
  val awsConfigs = mock[AWSConfigService]
  val requiredOAuth2ApiConfigs = mock[RequiredOAuth2ApiConfigService]
  val requiredSimpleTokenApis = mock[RequiredSimpleTokenApiService]
  val conversations = mock[ConversationService]
  val collectedParameterValues = mock[CollectedParameterValueService]
  val scheduledMessages = mock[ScheduledMessageService]
  val scheduledBehaviors = mock[ScheduledBehaviorService]
  val recurrences = mock[RecurrenceService]
  val invocationLogEntries = mock[InvocationLogEntryService]
  val behaviorResponses = mock[BehaviorResponseService]

  private def dontCallMe = throw new Exception("Don't call me")

  def run[T](action: DBIO[T]): Future[T] = dontCallMe
  def runNow[T](action: DBIO[T]): T = dontCallMe
  def runNow[T](future: Future[T]): T = dontCallMe
}

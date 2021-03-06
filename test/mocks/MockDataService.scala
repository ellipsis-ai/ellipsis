package mocks

import javax.inject.Singleton
import models.accounts.github.profile.GithubProfileService
import models.accounts.linkedaccount.LinkedAccountService
import models.accounts.linkedoauth1token.LinkedOAuth1TokenService
import models.accounts.linkedoauth2token.LinkedOAuth2TokenService
import models.accounts.linkedsimpletoken.LinkedSimpleTokenService
import models.accounts.logintoken.LoginTokenService
import models.accounts.ms_teams.botprofile.MSTeamsBotProfileService
import models.accounts.oauth1api.OAuth1ApiService
import models.accounts.oauth1application.OAuth1ApplicationService
import models.accounts.oauth1token.OAuth1TokenService
import models.accounts.oauth1tokenshare.OAuth1TokenShareService
import models.accounts.oauth2api.OAuth2ApiService
import models.accounts.oauth2application.OAuth2ApplicationService
import models.accounts.oauth2token.OAuth2TokenService
import models.accounts.oauth2tokenshare.OAuth2TokenShareService
import models.accounts.simpletokenapi.SimpleTokenApiService
import models.accounts.slack.botprofile.SlackBotProfileService
import models.accounts.slack.slackmemberstatus.SlackMemberStatusService
import models.accounts.user.UserService
import models.apitoken.APITokenService
import models.behaviors.BehaviorResponseService
import models.behaviors.behavior.BehaviorService
import models.behaviors.behaviorgroup.BehaviorGroupService
import models.behaviors.behaviorgroupdeployment.BehaviorGroupDeploymentService
import models.behaviors.behaviorgroupversion.BehaviorGroupVersionService
import models.behaviors.behaviorgroupversionsha.BehaviorGroupVersionSHAService
import models.behaviors.behaviorparameter.BehaviorParameterService
import models.behaviors.behaviortestresult.BehaviorTestResultService
import models.behaviors.behaviorversion.BehaviorVersionService
import models.behaviors.behaviorversionuserinvolvement.BehaviorVersionUserInvolvementService
import models.behaviors.config.awsconfig.AWSConfigService
import models.behaviors.config.requiredawsconfig.RequiredAWSConfigService
import models.behaviors.config.requiredoauth1apiconfig.RequiredOAuth1ApiConfigService
import models.behaviors.config.requiredoauth2apiconfig.RequiredOAuth2ApiConfigService
import models.behaviors.config.requiredsimpletokenapi.RequiredSimpleTokenApiService
import models.behaviors.conversations.collectedparametervalue.CollectedParameterValueService
import models.behaviors.conversations.conversation.ConversationService
import models.behaviors.conversations.parentconversation.ParentConversationService
import models.behaviors.datatypeconfig.DataTypeConfigService
import models.behaviors.datatypefield.DataTypeFieldService
import models.behaviors.defaultstorageitem.DefaultStorageItemService
import models.behaviors.input.InputService
import models.behaviors.invocationlogentry.InvocationLogEntryService
import models.behaviors.invocationtoken.InvocationTokenService
import models.behaviors.library.LibraryVersionService
import models.behaviors.linked_github_repo.LinkedGithubRepoService
import models.behaviors.managedbehaviorgroup.ManagedBehaviorGroupService
import models.behaviors.messagelistener.MessageListenerService
import models.behaviors.nodemoduleversion.NodeModuleVersionService
import models.behaviors.savedanswer.SavedAnswerService
import models.behaviors.scheduling.recurrence.RecurrenceService
import models.behaviors.scheduling.scheduledbehavior.ScheduledBehaviorService
import models.behaviors.scheduling.scheduledmessage.ScheduledMessageService
import models.behaviors.triggers.TriggerService
import models.billing.active_user_record.ActiveUserRecordService
import models.billing.addon.AddonService
import models.billing.invoice.InvoiceService
import models.billing.plan.PlanService
import models.billing.subscription.SubscriptionService
import models.devmodechannel.DevModeChannelService
import models.environmentvariable.TeamEnvironmentVariableService
import models.organization.OrganizationService
import models.team.TeamService
import org.scalatest.mockito.MockitoSugar
import services.DataService
import slick.dbio.DBIO

import scala.concurrent.Future

@Singleton
class MockDataService extends DataService with MockitoSugar {

  val users = mock[UserService]
  val loginTokens = mock[LoginTokenService]
  val linkedAccounts = mock[LinkedAccountService]
  val organizations = mock[OrganizationService]
  val teams = mock[TeamService]
  val apiTokens = mock[APITokenService]
  val teamEnvironmentVariables = mock[TeamEnvironmentVariableService]
  val invocationTokens = mock[InvocationTokenService]
  val linkedOAuth1Tokens = mock[LinkedOAuth1TokenService]
  val linkedOAuth2Tokens = mock[LinkedOAuth2TokenService]
  val linkedSimpleTokens = mock[LinkedSimpleTokenService]
  val oauth1TokenShares = mock[OAuth1TokenShareService]
  val oauth2TokenShares = mock[OAuth2TokenShareService]
  val oauth1Apis = mock[OAuth1ApiService]
  val oauth1Applications = mock[OAuth1ApplicationService]
  val oauth2Apis = mock[OAuth2ApiService]
  val oauth2Applications = mock[OAuth2ApplicationService]
  val simpleTokenApis = mock[SimpleTokenApiService]
  val githubProfiles = mock[GithubProfileService]
  val slackBotProfiles = mock[SlackBotProfileService]
  val msTeamsBotProfiles: MSTeamsBotProfileService = mock[MSTeamsBotProfileService]
  val oauth1Tokens = mock[OAuth1TokenService]
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
  val triggers = mock[TriggerService]
  val messageListeners = mock[MessageListenerService]
  val awsConfigs = mock[AWSConfigService]
  val requiredAWSConfigs: RequiredAWSConfigService = mock[RequiredAWSConfigService]
  val requiredOAuth1ApiConfigs = mock[RequiredOAuth1ApiConfigService]
  val requiredOAuth2ApiConfigs = mock[RequiredOAuth2ApiConfigService]
  val requiredSimpleTokenApis = mock[RequiredSimpleTokenApiService]
  val linkedGithubRepos = mock[LinkedGithubRepoService]
  val conversations = mock[ConversationService]
  val parentConversations = mock[ParentConversationService]
  val collectedParameterValues = mock[CollectedParameterValueService]
  val scheduledMessages = mock[ScheduledMessageService]
  val scheduledBehaviors = mock[ScheduledBehaviorService]
  val recurrences = mock[RecurrenceService]
  val invocationLogEntries = mock[InvocationLogEntryService]
  val behaviorVersionUserInvolvements = mock[BehaviorVersionUserInvolvementService]
  val devModeChannels = mock[DevModeChannelService]
  val behaviorGroupDeployments = mock[BehaviorGroupDeploymentService]
  val managedBehaviorGroups: ManagedBehaviorGroupService = mock[ManagedBehaviorGroupService]
  val behaviorGroupVersionSHAs = mock[BehaviorGroupVersionSHAService]
  val slackMemberStatuses = mock[SlackMemberStatusService]
  val behaviorTestResults = mock[BehaviorTestResultService]
  val behaviorResponses = mock[BehaviorResponseService]
  val subscriptions = mock[SubscriptionService]
  val plans = mock[PlanService]
  val invoices = mock[InvoiceService]
  val addons = mock[AddonService]
  val activeUserRecords = mock[ActiveUserRecordService]

  private def dontCallMe = throw new Exception("Don't call me")

  def run[T](action: DBIO[T]): Future[T] = dontCallMe
  def runNow[T](action: DBIO[T]): T = dontCallMe
  def runNow[T](future: Future[T]): T = dontCallMe
}

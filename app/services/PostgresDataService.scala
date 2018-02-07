package services

import javax.inject._

import models._
import models.accounts.github.profile.GithubProfileService
import models.accounts.linkedaccount.LinkedAccountService
import models.accounts.linkedoauth2token.LinkedOAuth2TokenService
import models.accounts.linkedsimpletoken.LinkedSimpleTokenService
import models.accounts.logintoken.LoginTokenService
import models.accounts.oauth2api.OAuth2ApiService
import models.accounts.oauth2application.OAuth2ApplicationService
import models.accounts.oauth2token.OAuth2TokenService
import models.accounts.simpletokenapi.SimpleTokenApiService
import models.accounts.slack.botprofile.SlackBotProfileService
import models.accounts.slack.profile.SlackProfileService
import models.accounts.user.UserService
import models.apitoken.APITokenService
import models.behaviors.BehaviorResponseService
import models.behaviors.behavior.BehaviorService
import models.behaviors.behaviorgroup.BehaviorGroupService
import models.behaviors.behaviorgroupdeployment.BehaviorGroupDeploymentService
import models.behaviors.behaviorgroupversion.BehaviorGroupVersionService
import models.behaviors.behaviorgroupversionsha.BehaviorGroupVersionSHAService
import models.behaviors.behaviorparameter.BehaviorParameterService
import models.behaviors.behaviorversion.BehaviorVersionService
import models.behaviors.config.awsconfig.AWSConfigService
import models.behaviors.config.requiredawsconfig.RequiredAWSConfigService
import models.behaviors.config.requiredoauth2apiconfig.RequiredOAuth2ApiConfigService
import models.behaviors.config.requiredsimpletokenapi.RequiredSimpleTokenApiService
import models.behaviors.conversations.collectedparametervalue.CollectedParameterValueService
import models.behaviors.conversations.conversation.ConversationService
import models.behaviors.datatypeconfig.DataTypeConfigService
import models.behaviors.datatypefield.DataTypeFieldService
import models.behaviors.defaultstorageitem.DefaultStorageItemService
import models.behaviors.input.InputService
import models.behaviors.invocationlogentry.InvocationLogEntryService
import models.behaviors.invocationtoken.InvocationTokenService
import models.behaviors.library.LibraryVersionService
import models.behaviors.linked_github_repo.LinkedGithubRepoService
import models.behaviors.nodemoduleversion.NodeModuleVersionService
import models.behaviors.savedanswer.SavedAnswerService
import models.behaviors.scheduling.recurrence.RecurrenceService
import models.behaviors.scheduling.scheduledbehavior.ScheduledBehaviorService
import models.behaviors.scheduling.scheduledmessage.ScheduledMessageService
import models.behaviors.triggers.messagetrigger.MessageTriggerService
import models.devmodechannel.DevModeChannelService
import models.environmentvariable.TeamEnvironmentVariableService
import models.organization.OrganizationService
import models.team.TeamService
import models.billing.subscription.SubscriptionService
import models.loggedevent.LoggedEventService
import slick.dbio.DBIO

import scala.concurrent.Future

@Singleton
class PostgresDataService @Inject() (
                                      val models: Models,
                                      val usersProvider: Provider[UserService],
                                      val loginTokensProvider: Provider[LoginTokenService],
                                      val linkedAccountsProvider: Provider[LinkedAccountService],
                                      val organizationsProvider: Provider[OrganizationService],
                                      val teamsProvider: Provider[TeamService],
                                      val apiTokensProvider: Provider[APITokenService],
                                      val environmentVariablesProvider: Provider[TeamEnvironmentVariableService],
                                      val invocationTokensProvider: Provider[InvocationTokenService],
                                      val linkedOAuth2TokensProvider: Provider[LinkedOAuth2TokenService],
                                      val linkedSimpleTokensProvider: Provider[LinkedSimpleTokenService],
                                      val oauth2ApisProvider: Provider[OAuth2ApiService],
                                      val oauth2ApplicationsProvider: Provider[OAuth2ApplicationService],
                                      val simpleTokenApisProvider: Provider[SimpleTokenApiService],
                                      val githubProfilesProvider: Provider[GithubProfileService],
                                      val slackProfilesProvider: Provider[SlackProfileService],
                                      val slackBotProfilesProvider: Provider[SlackBotProfileService],
                                      val oauth2TokensProvider: Provider[OAuth2TokenService],
                                      val behaviorGroupsProvider: Provider[BehaviorGroupService],
                                      val behaviorGroupVersionsProvider: Provider[BehaviorGroupVersionService],
                                      val behaviorsProvider: Provider[BehaviorService],
                                      val behaviorVersionsProvider: Provider[BehaviorVersionService],
                                      val dataTypeConfigsProvider: Provider[DataTypeConfigService],
                                      val dataTypeFieldsProvider: Provider[DataTypeFieldService],
                                      val defaultStorageItemsProvider: Provider[DefaultStorageItemService],
                                      val behaviorParametersProvider: Provider[BehaviorParameterService],
                                      val inputsProvider: Provider[InputService],
                                      val librariesProvider: Provider[LibraryVersionService],
                                      val nodeModuleVersionsProvider: Provider[NodeModuleVersionService],
                                      val savedAnswersProvider: Provider[SavedAnswerService],
                                      val messageTriggersProvider: Provider[MessageTriggerService],
                                      val awsConfigsProvider: Provider[AWSConfigService],
                                      val requiredAWSConfigsProvider: Provider[RequiredAWSConfigService],
                                      val requiredOAuth2ApiConfigsProvider: Provider[RequiredOAuth2ApiConfigService],
                                      val requiredSimpleTokenApiConfigsProvider: Provider[RequiredSimpleTokenApiService],
                                      val linkedGithubReposProvider: Provider[LinkedGithubRepoService],
                                      val conversationsProvider: Provider[ConversationService],
                                      val collectedParameterValuesProvider: Provider[CollectedParameterValueService],
                                      val scheduledMessagesProvider: Provider[ScheduledMessageService],
                                      val scheduledBehaviorsProvider: Provider[ScheduledBehaviorService],
                                      val recurrencesProvider: Provider[RecurrenceService],
                                      val invocationLogEntriesProvider: Provider[InvocationLogEntryService],
                                      val devModeChannelsProvider: Provider[DevModeChannelService],
                                      val behaviorGroupDeploymentsProvider: Provider[BehaviorGroupDeploymentService],
                                      val loggedEventsProvider: Provider[LoggedEventService],
                                      val behaviorGroupVersionSHAsProvider: Provider[BehaviorGroupVersionSHAService],
                                      val behaviorResponsesProvider: Provider[BehaviorResponseService],
                                      val subscriptionsProvider: Provider[SubscriptionService]
                                    ) extends DataService {

  val users = usersProvider.get
  val loginTokens = loginTokensProvider.get
  val linkedAccounts = linkedAccountsProvider.get
  val organizations = organizationsProvider.get
  val teams = teamsProvider.get
  val apiTokens = apiTokensProvider.get
  val teamEnvironmentVariables = environmentVariablesProvider.get
  val invocationTokens = invocationTokensProvider.get
  val linkedOAuth2Tokens = linkedOAuth2TokensProvider.get
  val linkedSimpleTokens = linkedSimpleTokensProvider.get
  val oauth2Apis = oauth2ApisProvider.get
  val oauth2Applications = oauth2ApplicationsProvider.get
  val simpleTokenApis = simpleTokenApisProvider.get
  val githubProfiles = githubProfilesProvider.get
  val slackProfiles = slackProfilesProvider.get
  val slackBotProfiles = slackBotProfilesProvider.get
  val oauth2Tokens = oauth2TokensProvider.get
  val behaviorGroups = behaviorGroupsProvider.get
  val behaviorGroupVersions = behaviorGroupVersionsProvider.get
  val behaviors = behaviorsProvider.get
  val behaviorVersions = behaviorVersionsProvider.get
  val dataTypeConfigs = dataTypeConfigsProvider.get
  val dataTypeFields = dataTypeFieldsProvider.get
  val defaultStorageItems = defaultStorageItemsProvider.get
  val behaviorParameters = behaviorParametersProvider.get
  val inputs = inputsProvider.get
  val libraries = librariesProvider.get
  val nodeModuleVersions = nodeModuleVersionsProvider.get
  val savedAnswers = savedAnswersProvider.get
  val messageTriggers = messageTriggersProvider.get
  val awsConfigs = awsConfigsProvider.get
  val requiredAWSConfigs = requiredAWSConfigsProvider.get
  val requiredOAuth2ApiConfigs = requiredOAuth2ApiConfigsProvider.get
  val requiredSimpleTokenApis = requiredSimpleTokenApiConfigsProvider.get
  val linkedGithubRepos = linkedGithubReposProvider.get
  val conversations = conversationsProvider.get
  val collectedParameterValues = collectedParameterValuesProvider.get
  val scheduledMessages = scheduledMessagesProvider.get
  val scheduledBehaviors = scheduledBehaviorsProvider.get
  val recurrences = recurrencesProvider.get
  val invocationLogEntries = invocationLogEntriesProvider.get
  val devModeChannels = devModeChannelsProvider.get
  val behaviorGroupDeployments = behaviorGroupDeploymentsProvider.get
  val loggedEvents = loggedEventsProvider.get
  val behaviorGroupVersionSHAs = behaviorGroupVersionSHAsProvider.get
  val subscriptions = subscriptionsProvider.get
  def behaviorResponses = behaviorResponsesProvider.get

  def run[T](action: DBIO[T]): Future[T] = models.run(action)
  def runNow[T](action: DBIO[T]): T = models.runNow(action)
  def runNow[T](future: Future[T]): T = models.runNow(future)
}

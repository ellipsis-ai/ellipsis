package services

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
import models.behaviors.behaviorgroupversion.BehaviorGroupVersionService
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
import models.billing.account.AccountService
import models.environmentvariable.TeamEnvironmentVariableService
import models.team.TeamService
import slick.dbio.DBIO

import scala.concurrent.Future

trait DataService {

  val users: UserService
  val loginTokens: LoginTokenService
  val linkedAccounts: LinkedAccountService
  val teams: TeamService
  val apiTokens: APITokenService
  val teamEnvironmentVariables: TeamEnvironmentVariableService
  val invocationTokens: InvocationTokenService
  val linkedOAuth2Tokens: LinkedOAuth2TokenService
  val linkedSimpleTokens: LinkedSimpleTokenService
  val oauth2Apis: OAuth2ApiService
  val oauth2Applications: OAuth2ApplicationService
  val simpleTokenApis: SimpleTokenApiService
  val githubProfiles: GithubProfileService
  val slackProfiles: SlackProfileService
  val slackBotProfiles: SlackBotProfileService
  val oauth2Tokens: OAuth2TokenService
  val behaviorGroups: BehaviorGroupService
  val behaviorGroupVersions: BehaviorGroupVersionService
  val behaviors: BehaviorService
  val behaviorVersions: BehaviorVersionService
  val dataTypeConfigs: DataTypeConfigService
  val dataTypeFields: DataTypeFieldService
  val defaultStorageItems: DefaultStorageItemService
  val behaviorParameters: BehaviorParameterService
  val inputs: InputService
  val libraries: LibraryVersionService
  val nodeModuleVersions: NodeModuleVersionService
  val savedAnswers: SavedAnswerService
  val messageTriggers: MessageTriggerService
  val awsConfigs: AWSConfigService
  val requiredAWSConfigs: RequiredAWSConfigService
  val requiredOAuth2ApiConfigs: RequiredOAuth2ApiConfigService
  val requiredSimpleTokenApis: RequiredSimpleTokenApiService
  val linkedGithubRepos: LinkedGithubRepoService
  val conversations: ConversationService
  val collectedParameterValues: CollectedParameterValueService
  val scheduledMessages: ScheduledMessageService
  val scheduledBehaviors: ScheduledBehaviorService
  val recurrences: RecurrenceService
  val invocationLogEntries: InvocationLogEntryService
  val billingAccounts: AccountService


  def behaviorResponses: BehaviorResponseService

  def run[T](action: DBIO[T]): Future[T]
  def runNow[T](action: DBIO[T]): T
  def runNow[T](future: Future[T]): T
}

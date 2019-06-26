package services

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
import slick.dbio.DBIO

import scala.concurrent.Future

trait DataService {

  val users: UserService
  val loginTokens: LoginTokenService
  val linkedAccounts: LinkedAccountService
  val organizations: OrganizationService
  val teams: TeamService
  val apiTokens: APITokenService
  val teamEnvironmentVariables: TeamEnvironmentVariableService
  val invocationTokens: InvocationTokenService
  val linkedOAuth1Tokens: LinkedOAuth1TokenService
  val linkedOAuth2Tokens: LinkedOAuth2TokenService
  val linkedSimpleTokens: LinkedSimpleTokenService
  val oauth1TokenShares: OAuth1TokenShareService
  val oauth2TokenShares: OAuth2TokenShareService
  val oauth1Apis: OAuth1ApiService
  val oauth1Applications: OAuth1ApplicationService
  val oauth2Apis: OAuth2ApiService
  val oauth2Applications: OAuth2ApplicationService
  val simpleTokenApis: SimpleTokenApiService
  val githubProfiles: GithubProfileService
  val slackBotProfiles: SlackBotProfileService
  val msTeamsBotProfiles: MSTeamsBotProfileService
  val oauth1Tokens: OAuth1TokenService
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
  val triggers: TriggerService
  val messageListeners: MessageListenerService
  val awsConfigs: AWSConfigService
  val requiredAWSConfigs: RequiredAWSConfigService
  val requiredOAuth1ApiConfigs: RequiredOAuth1ApiConfigService
  val requiredOAuth2ApiConfigs: RequiredOAuth2ApiConfigService
  val requiredSimpleTokenApis: RequiredSimpleTokenApiService
  val linkedGithubRepos: LinkedGithubRepoService
  val conversations: ConversationService
  val parentConversations: ParentConversationService
  val collectedParameterValues: CollectedParameterValueService
  val scheduledMessages: ScheduledMessageService
  val scheduledBehaviors: ScheduledBehaviorService
  val recurrences: RecurrenceService
  val invocationLogEntries: InvocationLogEntryService
  val behaviorVersionUserInvolvements: BehaviorVersionUserInvolvementService
  val devModeChannels: DevModeChannelService
  val subscriptions: SubscriptionService
  val invoices: InvoiceService
  val plans: PlanService
  val addons: AddonService
  val activeUserRecords: ActiveUserRecordService
  val behaviorGroupDeployments: BehaviorGroupDeploymentService
  val managedBehaviorGroups: ManagedBehaviorGroupService
  val behaviorGroupVersionSHAs: BehaviorGroupVersionSHAService
  val slackMemberStatuses: SlackMemberStatusService
  val behaviorTestResults: BehaviorTestResultService
  def behaviorResponses: BehaviorResponseService

  def run[T](action: DBIO[T]): Future[T]
  def runNow[T](action: DBIO[T]): T
  def runNow[T](future: Future[T]): T
}

package modules

import com.google.inject.AbstractModule
import models.Models
import models.accounts.github.profile.{GithubProfileService, GithubProfileServiceImpl}
import models.accounts.linkedaccount.{LinkedAccountService, LinkedAccountServiceImpl}
import models.accounts.linkedoauth1token.{LinkedOAuth1TokenService, LinkedOAuth1TokenServiceImpl}
import models.accounts.linkedoauth2token.{LinkedOAuth2TokenService, LinkedOAuth2TokenServiceImpl}
import models.accounts.linkedsimpletoken.{LinkedSimpleTokenService, LinkedSimpleTokenServiceImpl}
import models.accounts.logintoken.{LoginTokenService, LoginTokenServiceImpl}
import models.accounts.ms_teams.botprofile.{MSTeamsBotProfileService, MSTeamsBotProfileServiceImpl}
import models.accounts.oauth1api.{OAuth1ApiService, OAuth1ApiServiceImpl}
import models.accounts.oauth1application.{OAuth1ApplicationService, OAuth1ApplicationServiceImpl}
import models.accounts.oauth1token.{OAuth1TokenService, OAuth1TokenServiceImpl}
import models.accounts.oauth2api.{OAuth2ApiService, OAuth2ApiServiceImpl}
import models.accounts.oauth2application.{OAuth2ApplicationService, OAuth2ApplicationServiceImpl}
import models.accounts.oauth2token.{OAuth2TokenService, OAuth2TokenServiceImpl}
import models.accounts.registration.{RegistrationService, RegistrationServiceImpl}
import models.accounts.simpletokenapi.{SimpleTokenApiService, SimpleTokenApiServiceImpl}
import models.accounts.slack.botprofile.{SlackBotProfileService, SlackBotProfileServiceImpl}
import models.accounts.user.{UserService, UserServiceImpl}
import models.apitoken.{APITokenService, APITokenServiceImpl}
import models.behaviors.behavior.{BehaviorService, BehaviorServiceImpl}
import models.behaviors.behaviorgroup.{BehaviorGroupService, BehaviorGroupServiceImpl}
import models.behaviors.behaviorgroupdeployment.{BehaviorGroupDeploymentService, BehaviorGroupDeploymentServiceImpl}
import models.behaviors.behaviorgroupversion.{BehaviorGroupVersionService, BehaviorGroupVersionServiceImpl}
import models.behaviors.behaviorgroupversionsha.{BehaviorGroupVersionSHAService, BehaviorGroupVersionSHAServiceImpl}
import models.behaviors.behaviorparameter.{BehaviorParameterService, BehaviorParameterServiceImpl}
import models.behaviors.behaviortestresult.{BehaviorTestResultService, BehaviorTestResultServiceImpl}
import models.behaviors.behaviorversion.{BehaviorVersionService, BehaviorVersionServiceImpl}
import models.behaviors.config.awsconfig.{AWSConfigService, AWSConfigServiceImpl}
import models.behaviors.config.requiredawsconfig.{RequiredAWSConfigService, RequiredAWSConfigServiceImpl}
import models.behaviors.config.requiredoauth1apiconfig.{RequiredOAuth1ApiConfigService, RequiredOAuth1ApiConfigServiceImpl}
import models.behaviors.config.requiredoauth2apiconfig.{RequiredOAuth2ApiConfigService, RequiredOAuth2ApiConfigServiceImpl}
import models.behaviors.config.requiredsimpletokenapi.{RequiredSimpleTokenApiService, RequiredSimpleTokenApiServiceImpl}
import models.behaviors.conversations.collectedparametervalue.{CollectedParameterValueService, CollectedParameterValueServiceImpl}
import models.behaviors.conversations.conversation.{ConversationService, ConversationServiceImpl}
import models.behaviors.conversations.parentconversation.{ParentConversationService, ParentConversationServiceImpl}
import models.behaviors.datatypeconfig.{DataTypeConfigService, DataTypeConfigServiceImpl}
import models.behaviors.datatypefield.{DataTypeFieldService, DataTypeFieldServiceImpl}
import models.behaviors.defaultstorageitem.{DefaultStorageItemService, DefaultStorageItemServiceImpl}
import models.behaviors.events.EventHandler
import models.behaviors.input.{InputService, InputServiceImpl}
import models.behaviors.invocationlogentry.{InvocationLogEntryService, InvocationLogEntryServiceImpl}
import models.behaviors.invocationtoken.{InvocationTokenService, InvocationTokenServiceImpl}
import models.behaviors.library.{LibraryVersionService, LibraryVersionServiceImpl}
import models.behaviors.linked_github_repo.{LinkedGithubRepoService, LinkedGithubRepoServiceImpl}
import models.behaviors.managedbehaviorgroup.{ManagedBehaviorGroupService, ManagedBehaviorGroupServiceImpl}
import models.behaviors.messagelistener.{MessageListenerService, MessageListenerServiceImpl}
import models.behaviors.nodemoduleversion.{NodeModuleVersionService, NodeModuleVersionServiceImpl}
import models.behaviors.savedanswer.{SavedAnswerService, SavedAnswerServiceImpl}
import models.behaviors.scheduling.recurrence.{RecurrenceService, RecurrenceServiceImpl}
import models.behaviors.scheduling.scheduledbehavior.{ScheduledBehaviorService, ScheduledBehaviorServiceImpl}
import models.behaviors.scheduling.scheduledmessage.{ScheduledMessageService, ScheduledMessageServiceImpl}
import models.behaviors.triggers.{TriggerService, TriggerServiceImpl}
import models.behaviors.{BehaviorResponseService, BehaviorResponseServiceImpl, BotResultService, BotResultServiceImpl}
import models.billing.active_user_record.{ActiveUserRecordService, ActiveUserRecordServiceImpl}
import models.billing.addon.{AddonService, AddonServiceImpl}
import models.billing.customer.{CustomerService, CustomerServiceImpl}
import models.billing.invoice.{InvoiceService, InvoiceServiceImpl}
import models.billing.plan.{PlanService, PlanServiceImpl}
import models.billing.subscription.{SubscriptionService, SubscriptionServiceImpl}
import models.devmodechannel.{DevModeChannelService, DevModeChannelServiceImpl}
import models.environmentvariable._
import models.organization.{OrganizationService, OrganizationServiceImpl}
import models.team.{TeamService, TeamServiceImpl}
import net.codingwell.scalaguice.ScalaModule
import services._
import services.billing.{BillingService, BillingServiceImpl}
import services.caching.{CacheService, CacheServiceImpl}
import services.ms_teams.{MSTeamsApiService, MSTeamsEventService, MSTeamsEventServiceImpl}
import services.slack.{SlackApiService, SlackEventService, SlackEventServiceImpl}
import services.stats.{StatsService, StatsServiceImpl}
import utils.FileMap

class ServiceModule extends AbstractModule with ScalaModule {

  override def configure() = {
    bind[DataService].to[PostgresDataService]
    bind[UserService].to[UserServiceImpl]
    bind[LoginTokenService].to[LoginTokenServiceImpl]
    bind[LinkedAccountService].to[LinkedAccountServiceImpl]
    bind[RegistrationService].to[RegistrationServiceImpl]
    bind[OrganizationService].to[OrganizationServiceImpl]
    bind[TeamService].to[TeamServiceImpl]
    bind[APITokenService].to[APITokenServiceImpl]
    bind[TeamEnvironmentVariableService].to[TeamEnvironmentVariableServiceImpl]
    bind[InvocationTokenService].to[InvocationTokenServiceImpl]
    bind[LinkedOAuth1TokenService].to[LinkedOAuth1TokenServiceImpl]
    bind[LinkedOAuth2TokenService].to[LinkedOAuth2TokenServiceImpl]
    bind[LinkedSimpleTokenService].to[LinkedSimpleTokenServiceImpl]
    bind[OAuth1ApiService].to[OAuth1ApiServiceImpl]
    bind[OAuth1ApplicationService].to[OAuth1ApplicationServiceImpl]
    bind[OAuth2ApiService].to[OAuth2ApiServiceImpl]
    bind[OAuth2ApplicationService].to[OAuth2ApplicationServiceImpl]
    bind[SimpleTokenApiService].to[SimpleTokenApiServiceImpl]
    bind[GithubProfileService].to[GithubProfileServiceImpl]
    bind[SlackBotProfileService].to[SlackBotProfileServiceImpl]
    bind[MSTeamsBotProfileService].to[MSTeamsBotProfileServiceImpl]
    bind[OAuth1TokenService].to[OAuth1TokenServiceImpl]
    bind[OAuth2TokenService].to[OAuth2TokenServiceImpl]
    bind[BehaviorGroupService].to[BehaviorGroupServiceImpl]
    bind[BehaviorGroupVersionService].to[BehaviorGroupVersionServiceImpl]
    bind[BehaviorService].to[BehaviorServiceImpl]
    bind[BehaviorVersionService].to[BehaviorVersionServiceImpl]
    bind[DataTypeConfigService].to[DataTypeConfigServiceImpl]
    bind[DataTypeFieldService].to[DataTypeFieldServiceImpl]
    bind[DefaultStorageItemService].to[DefaultStorageItemServiceImpl]
    bind[BehaviorParameterService].to[BehaviorParameterServiceImpl]
    bind[InputService].to[InputServiceImpl]
    bind[LibraryVersionService].to[LibraryVersionServiceImpl]
    bind[SavedAnswerService].to[SavedAnswerServiceImpl]
    bind[TriggerService].to[TriggerServiceImpl]
    bind[MessageListenerService].to[MessageListenerServiceImpl]
    bind[AWSConfigService].to[AWSConfigServiceImpl]
    bind[RequiredAWSConfigService].to[RequiredAWSConfigServiceImpl]
    bind[RequiredOAuth1ApiConfigService].to[RequiredOAuth1ApiConfigServiceImpl]
    bind[RequiredOAuth2ApiConfigService].to[RequiredOAuth2ApiConfigServiceImpl]
    bind[RequiredSimpleTokenApiService].to[RequiredSimpleTokenApiServiceImpl]
    bind[LinkedGithubRepoService].to[LinkedGithubRepoServiceImpl]
    bind[ConversationService].to[ConversationServiceImpl]
    bind[ParentConversationService].to[ParentConversationServiceImpl]
    bind[CollectedParameterValueService].to[CollectedParameterValueServiceImpl]
    bind[ScheduledMessageService].to[ScheduledMessageServiceImpl]
    bind[ScheduledBehaviorService].to[ScheduledBehaviorServiceImpl]
    bind[RecurrenceService].to[RecurrenceServiceImpl]
    bind[InvocationLogEntryService].to[InvocationLogEntryServiceImpl]
    bind[AWSDynamoDBService].to[AWSDynamoDBServiceImpl]
    bind[BehaviorResponseService].to[BehaviorResponseServiceImpl]
    bind[BotResultService].to[BotResultServiceImpl]
    bind[NodeModuleVersionService].to[NodeModuleVersionServiceImpl]
    bind[DevModeChannelService].to[DevModeChannelServiceImpl]
    bind[BehaviorGroupDeploymentService].to[BehaviorGroupDeploymentServiceImpl]
    bind[ManagedBehaviorGroupService].to[ManagedBehaviorGroupServiceImpl]
    bind[BehaviorGroupVersionSHAService].to[BehaviorGroupVersionSHAServiceImpl]
    bind[BehaviorTestResultService].to[BehaviorTestResultServiceImpl]


    bind[AWSLambdaService].to[AWSLambdaServiceImpl]
    bind[AWSLogsService].to[AWSLogsServiceImpl]
    bind[CacheService].to[CacheServiceImpl]
    bind[GraphQLService].to[GraphQLServiceImpl]
    bind[SlackEventService].to[SlackEventServiceImpl]
    bind[MSTeamsEventService].to[MSTeamsEventServiceImpl]

    //  Services for billing
    bind[PlanService].to[PlanServiceImpl]
    bind[SubscriptionService].to[SubscriptionServiceImpl]
    bind[InvoiceService].to[InvoiceServiceImpl]
    bind[BillingService].to[BillingServiceImpl]
    bind[ActiveUserRecordService].to[ActiveUserRecordServiceImpl]
    bind[StatsService].to[StatsServiceImpl]
    bind[AddonService].to[AddonServiceImpl]
    bind[CustomerService].to[CustomerServiceImpl]

    bind[Models].asEagerSingleton()
    bind[PlanService].to[PlanServiceImpl]
    bind[EventHandler].asEagerSingleton()
    bind[GithubService].asEagerSingleton()
    bind[FileMap].asEagerSingleton()
    bind[SlackApiService].asEagerSingleton()
    bind[MSTeamsApiService].asEagerSingleton()
  }

}

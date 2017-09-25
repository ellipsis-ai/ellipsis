package modules

import com.google.inject.AbstractModule
import models.Models
import models.accounts.linkedaccount.{LinkedAccountService, LinkedAccountServiceImpl}
import models.accounts.linkedoauth2token.{LinkedOAuth2TokenService, LinkedOAuth2TokenServiceImpl}
import models.accounts.linkedsimpletoken.{LinkedSimpleTokenService, LinkedSimpleTokenServiceImpl}
import models.accounts.user.{UserService, UserServiceImpl}
import models.accounts.logintoken.{LoginTokenService, LoginTokenServiceImpl}
import models.accounts.oauth2api.{OAuth2ApiService, OAuth2ApiServiceImpl}
import models.accounts.oauth2application.{OAuth2ApplicationService, OAuth2ApplicationServiceImpl}
import models.accounts.slack.profile.{SlackProfileService, SlackProfileServiceImpl}
import models.accounts.oauth2token.{OAuth2TokenService, OAuth2TokenServiceImpl}
import models.accounts.simpletokenapi.{SimpleTokenApiService, SimpleTokenApiServiceImpl}
import models.accounts.slack.botprofile.{SlackBotProfileService, SlackBotProfileServiceImpl}
import models.apitoken.{APITokenService, APITokenServiceImpl}
import models.behaviors.{BehaviorResponseService, BehaviorResponseServiceImpl, BotResultService, BotResultServiceImpl}
import models.behaviors.behavior.{BehaviorService, BehaviorServiceImpl}
import models.behaviors.behaviorgroup.{BehaviorGroupService, BehaviorGroupServiceImpl}
import models.behaviors.behaviorgroupversion.{BehaviorGroupVersionService, BehaviorGroupVersionServiceImpl}
import models.behaviors.behaviorparameter.{BehaviorParameterService, BehaviorParameterServiceImpl}
import models.behaviors.behaviorversion.{BehaviorVersionService, BehaviorVersionServiceImpl}
import models.behaviors.config.awsconfig.{AWSConfigService, AWSConfigServiceImpl}
import models.behaviors.config.requiredawsconfig.{RequiredAWSConfigService, RequiredAWSConfigServiceImpl}
import models.behaviors.config.requiredoauth2apiconfig.{RequiredOAuth2ApiConfigService, RequiredOAuth2ApiConfigServiceImpl}
import models.behaviors.config.requiredsimpletokenapi.{RequiredSimpleTokenApiService, RequiredSimpleTokenApiServiceImpl}
import models.behaviors.conversations.collectedparametervalue.{CollectedParameterValueService, CollectedParameterValueServiceImpl}
import models.behaviors.conversations.conversation.{ConversationService, ConversationServiceImpl}
import models.behaviors.datatypeconfig.{DataTypeConfigService, DataTypeConfigServiceImpl}
import models.behaviors.datatypefield.{DataTypeFieldService, DataTypeFieldServiceImpl}
import models.behaviors.defaultstorageitem.{DefaultStorageItemService, DefaultStorageItemServiceImpl}
import models.behaviors.events.EventHandler
import models.behaviors.input.{InputService, InputServiceImpl}
import models.behaviors.invocationlogentry.{InvocationLogEntryService, InvocationLogEntryServiceImpl}
import models.behaviors.scheduling.scheduledmessage.{ScheduledMessageService, ScheduledMessageServiceImpl}
import models.behaviors.triggers.messagetrigger.{MessageTriggerService, MessageTriggerServiceImpl}
import models.environmentvariable._
import models.behaviors.invocationtoken.{InvocationTokenService, InvocationTokenServiceImpl}
import models.behaviors.library.{LibraryVersionService, LibraryVersionServiceImpl}
import models.behaviors.nodemoduleversion.{NodeModuleVersionService, NodeModuleVersionServiceImpl}
import models.behaviors.savedanswer.{SavedAnswerService, SavedAnswerServiceImpl}
import models.behaviors.scheduling.recurrence.{RecurrenceService, RecurrenceServiceImpl}
import models.behaviors.scheduling.scheduledbehavior.{ScheduledBehaviorService, ScheduledBehaviorServiceImpl}
import models.team.{TeamService, TeamServiceImpl}
import services._
import net.codingwell.scalaguice.ScalaModule

class ServiceModule extends AbstractModule with ScalaModule {

  override def configure() = {
    bind[DataService].to[PostgresDataService]

    bind[UserService].to[UserServiceImpl]
    bind[LoginTokenService].to[LoginTokenServiceImpl]
    bind[LinkedAccountService].to[LinkedAccountServiceImpl]
    bind[TeamService].to[TeamServiceImpl]
    bind[APITokenService].to[APITokenServiceImpl]
    bind[TeamEnvironmentVariableService].to[TeamEnvironmentVariableServiceImpl]
    bind[UserEnvironmentVariableService].to[UserEnvironmentVariableServiceImpl]
    bind[InvocationTokenService].to[InvocationTokenServiceImpl]
    bind[LinkedOAuth2TokenService].to[LinkedOAuth2TokenServiceImpl]
    bind[LinkedSimpleTokenService].to[LinkedSimpleTokenServiceImpl]
    bind[OAuth2ApiService].to[OAuth2ApiServiceImpl]
    bind[OAuth2ApplicationService].to[OAuth2ApplicationServiceImpl]
    bind[SimpleTokenApiService].to[SimpleTokenApiServiceImpl]
    bind[SlackProfileService].to[SlackProfileServiceImpl]
    bind[SlackBotProfileService].to[SlackBotProfileServiceImpl]
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
    bind[MessageTriggerService].to[MessageTriggerServiceImpl]
    bind[AWSConfigService].to[AWSConfigServiceImpl]
    bind[RequiredAWSConfigService].to[RequiredAWSConfigServiceImpl]
    bind[RequiredOAuth2ApiConfigService].to[RequiredOAuth2ApiConfigServiceImpl]
    bind[RequiredSimpleTokenApiService].to[RequiredSimpleTokenApiServiceImpl]
    bind[ConversationService].to[ConversationServiceImpl]
    bind[CollectedParameterValueService].to[CollectedParameterValueServiceImpl]
    bind[ScheduledMessageService].to[ScheduledMessageServiceImpl]
    bind[ScheduledBehaviorService].to[ScheduledBehaviorServiceImpl]
    bind[RecurrenceService].to[RecurrenceServiceImpl]
    bind[InvocationLogEntryService].to[InvocationLogEntryServiceImpl]
    bind[AWSDynamoDBService].to[AWSDynamoDBServiceImpl]
    bind[BehaviorResponseService].to[BehaviorResponseServiceImpl]
    bind[BotResultService].to[BotResultServiceImpl]
    bind[NodeModuleVersionService].to[NodeModuleVersionServiceImpl]

    bind[AWSLambdaService].to[AWSLambdaServiceImpl]
    bind[AWSLogsService].to[AWSLogsServiceImpl]
    bind[CacheService].to[CacheServiceImpl]
    bind[GraphQLService].to[GraphQLServiceImpl]
    bind[Models].asEagerSingleton()
    bind[SlackEventService].asEagerSingleton()
    bind[EventHandler].asEagerSingleton()
    bind[GithubService].asEagerSingleton()
  }

}

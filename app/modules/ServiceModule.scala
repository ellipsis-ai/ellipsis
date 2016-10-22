package modules

import com.google.inject.{AbstractModule, Provides}
import models.Models
import models.accounts.linkedaccount.{LinkedAccountService, LinkedAccountServiceImpl}
import models.accounts.linkedoauth2token.{LinkedOAuth2TokenService, LinkedOAuth2TokenServiceImpl}
import models.accounts.user.{UserService, UserServiceImpl}
import models.accounts.logintoken.{LoginTokenService, LoginTokenServiceImpl}
import models.accounts.oauth2api.{OAuth2ApiService, OAuth2ApiServiceImpl}
import models.accounts.oauth2application.{OAuth2ApplicationService, OAuth2ApplicationServiceImpl}
import models.accounts.slack.profile.{SlackProfileService, SlackProfileServiceImpl}
import models.accounts.oauth2token.{OAuth2TokenService, OAuth2TokenServiceImpl}
import models.accounts.slack.botprofile.{SlackBotProfileService, SlackBotProfileServiceImpl}
import models.apitoken.{APITokenService, APITokenServiceImpl}
import models.behaviors.behavior.{BehaviorService, BehaviorServiceImpl}
import models.behaviors.behaviorparameter.{BehaviorParameterService, BehaviorParameterServiceImpl}
import models.behaviors.behaviorversion.{BehaviorVersionService, BehaviorVersionServiceImpl}
import models.behaviors.config.awsconfig.{AWSConfigService, AWSConfigServiceImpl}
import models.behaviors.config.requiredoauth2apiconfig.{RequiredOAuth2ApiConfigService, RequiredOAuth2ApiConfigServiceImpl}
import models.behaviors.conversations.collectedparametervalue.{CollectedParameterValueService, CollectedParameterValueServiceImpl}
import models.behaviors.conversations.conversation.{ConversationService, ConversationServiceImpl}
import models.behaviors.events.EventHandler
import models.behaviors.invocationlogentry.{InvocationLogEntryService, InvocationLogEntryServiceImpl}
import models.behaviors.scheduledmessage.{ScheduledMessageService, ScheduledMessageServiceImpl}
import models.behaviors.triggers.messagetrigger.{MessageTriggerService, MessageTriggerServiceImpl}
import models.environmentvariable.{EnvironmentVariableService, EnvironmentVariableServiceImpl}
import models.behaviors.invocationtoken.{InvocationTokenService, InvocationTokenServiceImpl}
import models.team.{TeamService, TeamServiceImpl}
import play.api.Configuration
import play.api.i18n.MessagesApi
import services._
import net.codingwell.scalaguice.ScalaModule
import play.api.cache.CacheApi
import play.api.libs.ws.WSClient

class ServiceModule extends AbstractModule with ScalaModule {

  override def configure() = {
    bind[DataService].to(classOf[PostgresDataService])

    bind[UserService].to(classOf[UserServiceImpl])
    bind[LoginTokenService].to(classOf[LoginTokenServiceImpl])
    bind[LinkedAccountService].to(classOf[LinkedAccountServiceImpl])
    bind[TeamService].to(classOf[TeamServiceImpl])
    bind[APITokenService].to(classOf[APITokenServiceImpl])
    bind[EnvironmentVariableService].to(classOf[EnvironmentVariableServiceImpl])
    bind[InvocationTokenService].to(classOf[InvocationTokenServiceImpl])
    bind[LinkedOAuth2TokenService].to(classOf[LinkedOAuth2TokenServiceImpl])
    bind[OAuth2ApiService].to(classOf[OAuth2ApiServiceImpl])
    bind[OAuth2ApplicationService].to(classOf[OAuth2ApplicationServiceImpl])
    bind[SlackProfileService].to(classOf[SlackProfileServiceImpl])
    bind[SlackBotProfileService].to(classOf[SlackBotProfileServiceImpl])
    bind[OAuth2TokenService].to(classOf[OAuth2TokenServiceImpl])
    bind[BehaviorService].to(classOf[BehaviorServiceImpl])
    bind[BehaviorVersionService].to(classOf[BehaviorVersionServiceImpl])
    bind[BehaviorParameterService].to(classOf[BehaviorParameterServiceImpl])
    bind[MessageTriggerService].to(classOf[MessageTriggerServiceImpl])
    bind[AWSConfigService].to(classOf[AWSConfigServiceImpl])
    bind[RequiredOAuth2ApiConfigService].to(classOf[RequiredOAuth2ApiConfigServiceImpl])
    bind[ConversationService].to(classOf[ConversationServiceImpl])
    bind[CollectedParameterValueService].to(classOf[CollectedParameterValueServiceImpl])
    bind[ScheduledMessageService].to(classOf[ScheduledMessageServiceImpl])
    bind[InvocationLogEntryService].to(classOf[InvocationLogEntryServiceImpl])

    bind(classOf[AWSLambdaService]).to(classOf[AWSLambdaServiceImpl])
    bind(classOf[AWSLogsService]).to(classOf[AWSLogsServiceImpl])
    bind(classOf[Models]).asEagerSingleton()
    bind(classOf[SlackService]).to(classOf[SlackServiceImpl]).asEagerSingleton()
  }

  @Provides
  def provideAWSDynamoDBService(configuration: Configuration): AWSDynamoDBService = {
    new AWSDynamoDBServiceImpl(configuration)
  }

  @Provides
  def providesEventHandler(
                            lambdaService: AWSLambdaService,
                            dataService: DataService,
                            cache: CacheApi,
                            messages: MessagesApi,
                            ws: WSClient,
                            configuration: Configuration
                            ): EventHandler = {
    new EventHandler(lambdaService, dataService, cache, messages, ws, configuration)
  }

}

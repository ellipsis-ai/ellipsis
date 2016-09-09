package modules

import com.google.inject.{AbstractModule, Provides}
import models.Models
import models.accounts.linkedaccount.{LinkedAccountService, LinkedAccountServiceImpl}
import models.accounts.linkedoauth2token.{LinkedOAuth2TokenService, LinkedOAuth2TokenServiceImpl}
import models.accounts.user.{UserService, UserServiceImpl}
import models.accounts.logintoken.{LoginTokenService, LoginTokenServiceImpl}
import models.accounts.oauth2api.{OAuth2ApiService, OAuth2ApiServiceImpl}
import models.accounts.oauth2application.{OAuth2ApplicationService, OAuth2ApplicationServiceImpl}
import models.apitoken.{APITokenService, APITokenServiceImpl}
import models.bots.BehaviorTestReportBuilder
import models.bots.events.EventHandler
import models.environmentvariable.{EnvironmentVariableService, EnvironmentVariableServiceImpl}
import models.invocationtoken.{InvocationTokenService, InvocationTokenServiceImpl}
import models.team.{TeamService, TeamServiceImpl}
import play.api.Configuration
import play.api.cache.CacheApi
import play.api.i18n.MessagesApi
import play.api.libs.ws.WSClient
import services._
import net.codingwell.scalaguice.ScalaModule

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
    bind(classOf[Models]).asEagerSingleton()
    bind(classOf[SlackService]).asEagerSingleton()
    bind(classOf[BehaviorTestReportBuilder]).asEagerSingleton()
  }

  @Provides
  def provideAWSLambdaService(
                               configuration: Configuration,
                               models: Models,
                               ws: WSClient,
                               cache: CacheApi,
                               dataService: DataService
                             ): AWSLambdaService = {
    new AWSLambdaServiceImpl(configuration, models, ws, cache, dataService)
  }

  @Provides
  def provideAWSDynamoDBService(configuration: Configuration): AWSDynamoDBService = {
    new AWSDynamoDBServiceImpl(configuration)
  }

  @Provides
  def providesEventHandler(
                            lambdaService: AWSLambdaService,
                            dataService: DataService,
                            messages: MessagesApi
                            ): EventHandler = {
    new EventHandler(lambdaService, dataService, messages)
  }

}

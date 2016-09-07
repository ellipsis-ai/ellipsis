package modules

import com.google.inject.{AbstractModule, Provides}
import models.Models
import models.accounts.linkedaccount.{LinkedAccountService, LinkedAccountServiceImpl}
import models.accounts.user.{UserService, UserServiceImpl}
import models.accounts.logintoken.{LoginTokenService, LoginTokenServiceImpl}
import models.apitoken.{APITokenService, APITokenServiceImpl}
import models.bots.BehaviorTestReportBuilder
import models.bots.events.EventHandler
import models.environmentvariable.{EnvironmentVariableService, EnvironmentVariableServiceImpl}
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

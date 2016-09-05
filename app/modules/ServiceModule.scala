package modules

import com.google.inject.{AbstractModule, Provides}
import models.Models
import models.accounts.user.{UserService, UserServiceImpl}
import models.accounts.logintoken.{LoginTokenService, LoginTokenServiceImpl}
import models.bots.BehaviorTestReportBuilder
import models.bots.events.EventHandler
import play.api.Configuration
import play.api.cache.CacheApi
import play.api.i18n.MessagesApi
import play.api.libs.ws.WSClient
import services._
import net.codingwell.scalaguice.ScalaModule

class ServiceModule extends AbstractModule with ScalaModule {

  override def configure() = {
    bind[UserService].to[UserServiceImpl]
    bind(classOf[Models]).asEagerSingleton()
    bind(classOf[SlackService]).asEagerSingleton()
    bind(classOf[BehaviorTestReportBuilder]).asEagerSingleton()
  }

  @Provides
  def providesDataService(models: Models, userService: UserService, loginTokenService: LoginTokenService): DataService = {
    new PostgresDataService(models, userService, loginTokenService)
  }

  @Provides
  def providesLoginTokenService(models: Models): LoginTokenService = {
    new LoginTokenServiceImpl(models)
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

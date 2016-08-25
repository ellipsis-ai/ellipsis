package modules

import com.google.inject.{AbstractModule, Provides}
import models.Models
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
    bind(classOf[Models]).asEagerSingleton()
    bind(classOf[SlackService]).asEagerSingleton()
    bind(classOf[BehaviorTestReportBuilder]).asEagerSingleton()
  }

  @Provides
  def provideAWSLambdaService(configuration: Configuration, models: Models, ws: WSClient, cache: CacheApi): AWSLambdaService = {
    new AWSLambdaServiceImpl(configuration, models, ws, cache)
  }

  @Provides
  def provideAWSDynamoDBService(configuration: Configuration): AWSDynamoDBService = {
    new AWSDynamoDBServiceImpl(configuration)
  }

  @Provides
  def providesEventHandler(
                            lambdaService: AWSLambdaService,
                            models: Models,
                            messages: MessagesApi
                            ): EventHandler = {
    new EventHandler(lambdaService, models, messages)
  }

}

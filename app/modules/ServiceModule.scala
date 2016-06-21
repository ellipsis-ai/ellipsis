package modules

import com.google.inject.{Provides, AbstractModule}
import models.Models
import models.bots.{BehaviorTestReportBuilder, EventHandler}
import play.api.Configuration
import play.api.i18n.MessagesApi
import services._
import net.codingwell.scalaguice.ScalaModule


class ServiceModule extends AbstractModule with ScalaModule {

  override def configure() = {
    bind(classOf[Models]).asEagerSingleton()
    bind(classOf[SlackService]).asEagerSingleton()
    bind(classOf[BehaviorTestReportBuilder]).asEagerSingleton()
  }

  @Provides
  def provideAWSLambdaService(configuration: Configuration, models: Models): AWSLambdaService = {
    new AWSLambdaServiceImpl(configuration, models)
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

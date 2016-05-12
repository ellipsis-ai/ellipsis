package modules

import com.google.inject.{Provides, AbstractModule}
import models.Models
import play.api.Configuration
import services._
import net.codingwell.scalaguice.ScalaModule


class ServiceModule extends AbstractModule with ScalaModule {

  override def configure() = {
    bind(classOf[Models]).asEagerSingleton()
    bind(classOf[SlackService]).asEagerSingleton()
  }

  @Provides
  def provideAWSLambdaService(configuration: Configuration, models: Models): AWSLambdaService = {
    new AWSLambdaServiceImpl(configuration, models)
  }

  @Provides
  def provideAWSDynamoDBService(configuration: Configuration): AWSDynamoDBService = {
    new AWSDynamoDBServiceImpl(configuration)
  }

}

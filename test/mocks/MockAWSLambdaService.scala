package mocks

import javax.inject.Inject

import com.amazonaws.services.lambda.AWSLambdaAsyncClient
import models.Models
import models.behaviors.behaviorversion.BehaviorVersion
import models.behaviors.config.awsconfig.AWSConfig
import models.behaviors.config.requiredoauth2apiconfig.RequiredOAuth2ApiConfig
import models.behaviors.config.requiredsimpletokenapi.RequiredSimpleTokenApi
import models.behaviors.conversations.conversation.Conversation
import models.behaviors.events.Event
import models.behaviors.library.LibraryVersion
import models.behaviors.{BotResult, ParameterWithValue}
import models.environmentvariable.EnvironmentVariable
import org.scalatest.mock.MockitoSugar
import play.api.Configuration
import play.api.libs.ws.WSClient
import services.{AWSLambdaService, AWSLogsService, DataService}
import slick.dbio.DBIO

import scala.concurrent.Future

class MockAWSLambdaService @Inject() (
                                       val configuration: Configuration,
                                       val models: Models,
                                       val ws: WSClient,
                                       val dataService: DataService,
                                       val logsService: AWSLogsService
                                     ) extends AWSLambdaService with MockitoSugar {

  override val client: AWSLambdaAsyncClient = mock[AWSLambdaAsyncClient]

  override def listBehaviorFunctionNames: Future[Seq[String]] = Future.successful(Seq())

  def partionedBehaviorFunctionNames: Future[PartitionedFunctionNames] = {
    Future.successful(PartitionedFunctionNames(Seq(), Seq(), Seq()))
  }

  override def deleteFunction(functionName: String): Future[Unit] = Future.successful({})

  override def deployFunctionFor(
                                  behaviorVersion: BehaviorVersion,
                                  functionBody: String,
                                  params: Array[String],
                                  libraries: Seq[LibraryVersion],
                                  maybeAWSConfig: Option[AWSConfig],
                                  requiredOAuth2ApiConfigs: Seq[RequiredOAuth2ApiConfig],
                                  requiredSimpleTokenApis: Seq[RequiredSimpleTokenApi]
                                ): Future[Unit] = Future.successful({})

  override def invoke(
                       behaviorVersion: BehaviorVersion,
                       parametersWithValues: Seq[ParameterWithValue],
                       environmentVariables: Seq[EnvironmentVariable],
                       event: Event,
                       maybeConversation: Option[Conversation]
                     ): Future[BotResult] = Future.successful(mock[BotResult])

  override def invokeAction(
                       behaviorVersion: BehaviorVersion,
                       parametersWithValues: Seq[ParameterWithValue],
                       environmentVariables: Seq[EnvironmentVariable],
                       event: Event,
                       maybeConversation: Option[Conversation]
                     ): DBIO[BotResult] = DBIO.successful(mock[BotResult])

  override def functionWithParams(params: Array[String], functionBody: String): String = ""
}

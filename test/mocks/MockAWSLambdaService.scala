package mocks

import javax.inject.Inject

import com.amazonaws.services.lambda.AWSLambdaAsyncClient
import models.Models
import models.behaviors.behaviorgroupversion.BehaviorGroupVersion
import models.behaviors.behaviorversion.BehaviorVersion
import models.behaviors.config.awsconfig.AWSConfig
import models.behaviors.config.requiredoauth2apiconfig.RequiredOAuth2ApiConfig
import models.behaviors.config.requiredsimpletokenapi.RequiredSimpleTokenApi
import models.behaviors.conversations.conversation.Conversation
import models.behaviors.events.Event
import models.behaviors.library.LibraryVersion
import models.behaviors.nodemoduleversion.NodeModuleVersion
import models.behaviors.{BotResult, ParameterWithValue, SuccessResult}
import models.environmentvariable.EnvironmentVariable
import org.scalatest.mock.MockitoSugar
import play.api.Configuration
import play.api.libs.json.{JsNull, JsString}
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

  def resultFor(event: Event, maybeConversation: Option[Conversation]): BotResult = {
    SuccessResult(
      event,
      maybeConversation,
      result = JsString("result"),
      resultWithOptions = JsNull,
      parametersWithValues = Seq(),
      maybeResponseTemplate = None,
      maybeLogResult = None,
      forcePrivateResponse = false
    )
  }

  override val client: AWSLambdaAsyncClient = mock[AWSLambdaAsyncClient]

  override def listBehaviorGroupFunctionNames: Future[Seq[String]] = Future.successful(Seq())

  def partionedBehaviorGroupFunctionNames: Future[PartitionedFunctionNames] = {
    Future.successful(PartitionedFunctionNames(Seq(), Seq(), Seq()))
  }

  override def deleteFunction(functionName: String): Future[Unit] = Future.successful({})

  override def deployFunctionFor(
                                  groupVersion: BehaviorGroupVersion,
                                  libraries: Seq[LibraryVersion],
                                  behaviorVersionsWithParams: Seq[(BehaviorVersion, Array[String])],
                                  maybeAWSConfig: Option[AWSConfig],
                                  requiredOAuth2ApiConfigs: Seq[RequiredOAuth2ApiConfig],
                                  requiredSimpleTokenApis: Seq[RequiredSimpleTokenApi],
                                  forceNodeModuleUpdate: Boolean
                                ): Future[Unit] = Future.successful({})

  override def invokeAction(
                       behaviorVersion: BehaviorVersion,
                       parametersWithValues: Seq[ParameterWithValue],
                       environmentVariables: Seq[EnvironmentVariable],
                       event: Event,
                       maybeConversation: Option[Conversation]
                     ): DBIO[BotResult] = DBIO.successful(resultFor(event, maybeConversation))

  override def functionWithParams(params: Array[String], functionBody: String): String = ""

  def ensureNodeModuleVersionsFor(groupVersion: BehaviorGroupVersion): DBIO[Seq[NodeModuleVersion]] = DBIO.successful(Seq())
}

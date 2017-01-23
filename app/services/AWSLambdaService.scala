package services

import com.amazonaws.services.lambda.AWSLambdaAsyncClient
import models.Models
import models.behaviors.behaviorversion.BehaviorVersion
import models.behaviors.config.awsconfig.AWSConfig
import models.behaviors.config.requiredoauth2apiconfig.RequiredOAuth2ApiConfig
import models.behaviors.config.requiredsimpletokenapi.RequiredSimpleTokenApi
import models.behaviors.events.MessageEvent
import models.behaviors.{BotResult, ParameterWithValue}
import models.environmentvariable.EnvironmentVariable
import play.api.Configuration

import scala.concurrent.Future

trait AWSLambdaService extends AWSService {

  val configuration: Configuration
  val models: Models

  val client: AWSLambdaAsyncClient

  def listFunctionNames: Future[Seq[String]]

  case class PartitionedFunctionNames(current: Seq[String], missing: Seq[String], obsolete: Seq[String])

  def partionedFunctionNames: Future[PartitionedFunctionNames]

  def functionWithParams(params: Array[String], functionBody: String): String

  def invoke(
              behaviorVersion: BehaviorVersion,
              parametersWithValues: Seq[ParameterWithValue],
              environmentVariables: Seq[EnvironmentVariable],
              event: MessageEvent
              ): Future[BotResult]

  def deleteFunction(functionName: String): Future[Unit]
  def deployFunctionFor(
                         behaviorVersion: BehaviorVersion,
                         functionBody: String,
                         params: Array[String],
                         maybeAWSConfig: Option[AWSConfig],
                         requiredOAuth2ApiConfigs: Seq[RequiredOAuth2ApiConfig],
                         requiredSimpleTokenApis: Seq[RequiredSimpleTokenApi]
                         ): Future[Unit]

}

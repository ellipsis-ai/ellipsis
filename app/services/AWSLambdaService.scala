package services

import com.amazonaws.services.lambda.AWSLambdaAsyncClient
import models.bots.config.{AWSConfig, RequiredOAuth2Application}
import models.bots.events.MessageEvent
import models.{EnvironmentVariable, Models}
import models.bots.{BehaviorResult, BehaviorVersion, ParameterWithValue}
import play.api.Configuration

import scala.concurrent.Future

trait AWSLambdaService extends AWSService {

  val configuration: Configuration
  val models: Models

  val client: AWSLambdaAsyncClient

  def listFunctionNames: Future[Seq[String]]

  def invoke(
              behaviorVersion: BehaviorVersion,
              parametersWithValues: Seq[ParameterWithValue],
              environmentVariables: Seq[EnvironmentVariable],
              event: MessageEvent
              ): Future[BehaviorResult]

  def deleteFunction(functionName: String): Unit
  def deployFunctionFor(
                         behaviorVersion: BehaviorVersion,
                         functionBody: String,
                         params: Array[String],
                         maybeAWSConfig: Option[AWSConfig],
                         requiredOAuth2Applications: Seq[RequiredOAuth2Application]
                         ): Future[Unit]

}

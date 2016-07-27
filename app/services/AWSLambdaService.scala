package services

import com.amazonaws.services.lambda.AWSLambdaAsyncClient
import models.bots.config.AWSConfig
import models.{EnvironmentVariable, Models}
import models.bots.{BehaviorResult, ParameterWithValue, BehaviorVersion}
import play.api.Configuration

import scala.concurrent.Future

trait AWSLambdaService extends AWSService {

  val configuration: Configuration
  val models: Models

  val client: AWSLambdaAsyncClient

  def invoke(behaviorVersion: BehaviorVersion, parametersWithValues: Seq[ParameterWithValue], environmentVariables: Seq[EnvironmentVariable]): Future[BehaviorResult]

  def deleteFunction(functionName: String): Unit
  def deployFunctionFor(behaviorVersion: BehaviorVersion, functionBody: String, params: Array[String], maybeAWSConfig: Option[AWSConfig]): Future[Unit]

}

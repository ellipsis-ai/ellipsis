package services

import com.amazonaws.services.lambda.AWSLambdaAsyncClient
import com.amazonaws.services.lambda.model.InvokeResult
import models.behaviors.events.MessageEvent
import models.Models
import models.behaviors.behaviorversion.BehaviorVersion
import models.behaviors.config.awsconfig.AWSConfig
import models.behaviors.config.requiredoauth2apiconfig.RequiredOAuth2ApiConfig
import models.behaviors.{BotResult, ParameterWithValue}
import models.environmentvariable.EnvironmentVariable
import models.team.Team
import play.api.Configuration
import play.api.libs.json.JsValue

import scala.concurrent.Future

trait AWSLambdaService extends AWSService {

  val configuration: Configuration
  val models: Models

  val client: AWSLambdaAsyncClient

  def listFunctionNames: Future[Seq[String]]

  def functionWithParams(params: Array[String], functionBody: String): String

  def invokeFunction(
                      functionName: String,
                      payloadData: Seq[(String, JsValue)],
                      team: Team,
                      event: MessageEvent,
                      requiredOAuth2ApiConfigs: Seq[RequiredOAuth2ApiConfig],
                      environmentVariables: Seq[EnvironmentVariable],
                      successFn: InvokeResult => BotResult
                    ): Future[BotResult]

  def invoke(
              behaviorVersion: BehaviorVersion,
              parametersWithValues: Seq[ParameterWithValue],
              environmentVariables: Seq[EnvironmentVariable],
              event: MessageEvent
              ): Future[BotResult]

  def deleteFunction(functionName: String): Unit
  def deployFunctionFor(
                         behaviorVersion: BehaviorVersion,
                         functionBody: String,
                         params: Array[String],
                         maybeAWSConfig: Option[AWSConfig],
                         requiredOAuth2ApiConfigs: Seq[RequiredOAuth2ApiConfig]
                         ): Future[Unit]

}

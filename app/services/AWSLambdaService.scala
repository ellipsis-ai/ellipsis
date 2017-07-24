package services

import com.amazonaws.services.lambda.AWSLambdaAsync
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
import play.api.Configuration
import slick.dbio.DBIO

import scala.concurrent.Future

trait AWSLambdaService extends AWSService {

  val configuration: Configuration
  val models: Models

  val client: AWSLambdaAsync

  def listBehaviorFunctionNames: Future[Seq[String]]

  case class PartitionedFunctionNames(current: Seq[String], missing: Seq[String], obsolete: Seq[String])

  def partionedBehaviorFunctionNames: Future[PartitionedFunctionNames]

  def functionWithParams(params: Array[String], functionBody: String): String

  def invokeAction(
                    behaviorVersion: BehaviorVersion,
                    parametersWithValues: Seq[ParameterWithValue],
                    environmentVariables: Seq[EnvironmentVariable],
                    event: Event,
                    maybeConversation: Option[Conversation]
                  ): DBIO[BotResult]

  def invoke(
              behaviorVersion: BehaviorVersion,
              parametersWithValues: Seq[ParameterWithValue],
              environmentVariables: Seq[EnvironmentVariable],
              event: Event,
              maybeConversation: Option[Conversation]
              ): Future[BotResult]

  def deleteFunction(functionName: String): Future[Unit]
  def deployFunctionFor(
                         behaviorVersion: BehaviorVersion,
                         functionBody: String,
                         params: Array[String],
                         libraries: Seq[LibraryVersion],
                         maybeAWSConfig: Option[AWSConfig],
                         requiredOAuth2ApiConfigs: Seq[RequiredOAuth2ApiConfig],
                         requiredSimpleTokenApis: Seq[RequiredSimpleTokenApi]
                         ): Future[Unit]

}

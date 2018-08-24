package services

import akka.actor.ActorSystem
import com.amazonaws.services.lambda.AWSLambdaAsync
import models.behaviors.behaviorgroupversion.BehaviorGroupVersion
import models.behaviors.behaviorparameter.BehaviorParameter
import models.behaviors.behaviorversion.BehaviorVersion
import models.behaviors.config.awsconfig.AWSConfig
import models.behaviors.config.requiredawsconfig.RequiredAWSConfig
import models.behaviors.config.requiredoauth1apiconfig.RequiredOAuth1ApiConfig
import models.behaviors.config.requiredoauth2apiconfig.RequiredOAuth2ApiConfig
import models.behaviors.config.requiredsimpletokenapi.RequiredSimpleTokenApi
import models.behaviors.conversations.conversation.Conversation
import models.behaviors.events.Event
import models.behaviors.library.LibraryVersion
import models.behaviors.nodemoduleversion.NodeModuleVersion
import models.behaviors.{BotResult, ParameterWithValue}
import models.environmentvariable.EnvironmentVariable
import play.api.Configuration
import slick.dbio.DBIO

import scala.concurrent.{ExecutionContext, Future}

case class ApiConfigInfo(
                          awsConfigs: Seq[AWSConfig],
                          requiredAWSConfigs: Seq[RequiredAWSConfig],
                          requiredOAuth1ApiConfigs: Seq[RequiredOAuth1ApiConfig],
                          requiredOAuth2ApiConfigs: Seq[RequiredOAuth2ApiConfig],
                          requiredSimpleTokenApis: Seq[RequiredSimpleTokenApi]
                        )

trait AWSLambdaService extends AWSService {

  val configuration: Configuration

  val invocationTimeoutSeconds: Int = configuration.get[Int]("aws.lambda.timeoutSeconds")

  val client: AWSLambdaAsync

  def listBehaviorGroupFunctionNames: Future[Seq[String]]

  case class PartitionedFunctionNames(current: Seq[String], missing: Seq[String], obsolete: Seq[String])

  def partitionedBehaviorGroupFunctionNames: Future[PartitionedFunctionNames]

  def functionWithParams(params: Seq[BehaviorParameter], functionBody: String, isForExport: Boolean): String

  def invokeAction(
                    behaviorVersion: BehaviorVersion,
                    parametersWithValues: Seq[ParameterWithValue],
                    environmentVariables: Seq[EnvironmentVariable],
                    event: Event,
                    maybeConversation: Option[Conversation],
                    defaultServices: DefaultServices
                  )(implicit actorSystem: ActorSystem, ec: ExecutionContext): DBIO[BotResult]

  def deleteFunction(functionName: String): Future[Unit]
  def deployFunctionFor(
                         groupVersion: BehaviorGroupVersion,
                         behaviorVersionsWithParams: Seq[(BehaviorVersion, Seq[BehaviorParameter])],
                         libraries: Seq[LibraryVersion],
                         apiConfigInfo: ApiConfigInfo
                         ): Future[Unit]

  def ensureNodeModuleVersionsFor(groupVersion: BehaviorGroupVersion): DBIO[Seq[NodeModuleVersion]]
}

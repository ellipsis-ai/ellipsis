package models.behaviors.behaviorversion

import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.time.OffsetDateTime

import models.accounts.user.User
import models.behaviors._
import models.behaviors.behavior.Behavior
import models.behaviors.behaviorgroup.BehaviorGroup
import models.behaviors.behaviorgroupversion.BehaviorGroupVersion
import models.behaviors.conversations.conversation.Conversation
import models.behaviors.datatypeconfig.BehaviorVersionForDataTypeSchema
import models.behaviors.datatypefield.DataTypeFieldForSchema
import models.behaviors.defaultstorageitem.GraphQLHelpers
import models.behaviors.events.Event
import models.team.Team
import play.api.Configuration
import play.api.libs.json.{JsValue, Json}
import services.AWSLambdaConstants._
import services.{AWSLambdaLogResult, DataService}
import slick.dbio.DBIO

import scala.concurrent.{ExecutionContext, Future}

case class BehaviorVersion(
                            id: String,
                            behavior: Behavior,
                            groupVersion: BehaviorGroupVersion,
                            maybeDescription: Option[String],
                            maybeName: Option[String],
                            maybeFunctionBody: Option[String],
                            maybeResponseTemplate: Option[String],
                            forcePrivateResponse: Boolean,
                            maybeAuthor: Option[User],
                            createdAt: OffsetDateTime
                          ) extends BehaviorVersionForDataTypeSchema {

  lazy val jsName: String = s"${BehaviorVersion.dirName}/$id.js"

  lazy val typeName = maybeName.getOrElse(GraphQLHelpers.fallbackTypeName)

  def dataTypeFieldsAction(dataService: DataService)(implicit ec: ExecutionContext): DBIO[Seq[DataTypeFieldForSchema]] = {
    dataService.dataTypeConfigs.maybeForAction(this).flatMap { maybeConfig =>
      maybeConfig.map { config =>
        dataService.dataTypeFields.allForAction(config)
      }.getOrElse(DBIO.successful(Seq()))
    }
  }

  def dataTypeFields(dataService: DataService)(implicit ec: ExecutionContext): Future[Seq[DataTypeFieldForSchema]] = {
    dataService.run(dataTypeFieldsAction(dataService))
  }

  val maybeExportId: Option[String] = behavior.maybeExportId

  def isDataType: Boolean = behavior.isDataType

  def group: BehaviorGroup = behavior.group

  val team: Team = behavior.team

  val exportName: String = {
    maybeName.getOrElse(id)
  }

  def hasFunction: Boolean = {
    maybeFunctionBody.exists(_.trim.nonEmpty)
  }

  def description: String = maybeDescription.getOrElse("")

  def functionBody: String = maybeFunctionBody.getOrElse("")

  private def isSyntaxError(json: JsValue): Boolean = {
    (json \ "errorType").toOption.flatMap { m =>
      "SyntaxError".r.findFirstIn(m.toString)
    }.isDefined
  }

  def resultFor(
                 payload: ByteBuffer,
                 logResult: AWSLambdaLogResult,
                 parametersWithValues: Seq[ParameterWithValue],
                 dataService: DataService,
                 configuration: Configuration,
                 event: Event,
                 maybeConversation: Option[Conversation],
                 isForUndeployed: Boolean,
                 hasUndeployedVersionForAuthor: Boolean
               ): BotResult = {
    val bytes = payload.array
    val jsonString = new java.lang.String( bytes, Charset.forName("UTF-8") )
    val json = Json.parse(jsonString)
    val logResultOption = Some(logResult)
    (json \ "result").toOption.map { successResult =>
      SuccessResult(
        event,
        this,
        maybeConversation,
        successResult,
        json,
        parametersWithValues,
        maybeResponseTemplate,
        logResultOption,
        forcePrivateResponse,
        isForUndeployed,
        hasUndeployedVersionForAuthor
      )
    }.getOrElse {
      if ((json \ NO_RESPONSE_KEY).toOption.exists(_.as[Boolean])) {
        NoResponseResult(event, this, maybeConversation, json, logResultOption)
      } else {
        if (json.toString == "null") {
          NoCallbackTriggeredResult(event, maybeConversation, this, dataService, configuration, isForUndeployed, hasUndeployedVersionForAuthor)
        } else if (isSyntaxError(json)) {
          SyntaxErrorResult(event, maybeConversation, this, dataService, configuration, json, logResultOption, isForUndeployed, hasUndeployedVersionForAuthor)
        } else {
          ExecutionErrorResult(event, maybeConversation, this, dataService, configuration, json, logResultOption, isForUndeployed, hasUndeployedVersionForAuthor)
        }
      }
    }
  }

  def toRaw: RawBehaviorVersion = {
    RawBehaviorVersion(
      id,
      behavior.id,
      groupVersion.id,
      maybeDescription,
      maybeName,
      maybeFunctionBody,
      maybeResponseTemplate,
      forcePrivateResponse,
      maybeAuthor.map(_.id),
      createdAt
    )
  }

}

object BehaviorVersion {

  val dirName: String = "behavior_versions"

  def codeFor(functionBody: String): String = {
    s"module.exports = ${functionBody.trim};"
  }

}

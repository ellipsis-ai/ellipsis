package models.behaviors.behaviorversion

import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.time.OffsetDateTime

import models.accounts.user.User
import models.behaviors._
import models.behaviors.behavior.Behavior
import models.behaviors.events.MessageEvent
import models.team.Team
import play.api.libs.json.{JsValue, Json}
import play.api.Configuration
import services.AWSLambdaConstants._
import services.{AWSLambdaLogResult, DataService}

case class BehaviorVersion(
                            id: String,
                            behavior: Behavior,
                            maybeDescription: Option[String],
                            maybeName: Option[String],
                            maybeFunctionBody: Option[String],
                            maybeResponseTemplate: Option[String],
                            forcePrivateResponse: Boolean,
                            maybeAuthor: Option[User],
                            createdAt: OffsetDateTime
                          ) {

  val team: Team = behavior.team

  val exportName: String = {
    behavior.maybeDataTypeName.orElse(maybeName).getOrElse(id)
  }

  def isSkill: Boolean = {
    maybeFunctionBody.exists { body =>
      Option(body).exists(_.trim.nonEmpty)
    }
  }

  def description: String = maybeDescription.getOrElse("")

  def functionBody: String = maybeFunctionBody.getOrElse("")

  def functionName: String = id

  def isCurrentVersion: Boolean = behavior.maybeCurrentVersionId.contains(id)

  private def isUnhandledError(json: JsValue): Boolean = {
    (json \ "errorMessage").toOption.flatMap { m =>
      "Process exited before completing request".r.findFirstIn(m.toString)
    }.isDefined
  }

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
                 event: MessageEvent
               ): BotResult = {
    val bytes = payload.array
    val jsonString = new java.lang.String( bytes, Charset.forName("UTF-8") )
    val json = Json.parse(jsonString)
    val logResultOption = Some(logResult)
    (json \ "result").toOption.map { successResult =>
      SuccessResult(event, successResult, parametersWithValues, maybeResponseTemplate, logResultOption, forcePrivateResponse)
    }.getOrElse {
      if ((json \ NO_RESPONSE_KEY).toOption.exists(_.as[Boolean])) {
        NoResponseResult(event, logResultOption)
      } else {
        if (isUnhandledError(json)) {
          UnhandledErrorResult(event, this, dataService, configuration, logResultOption)
        } else if (json.toString == "null") {
          NoCallbackTriggeredResult(event, this, dataService, configuration)
        } else if (isSyntaxError(json)) {
          SyntaxErrorResult(event, this, dataService, configuration, json, logResultOption)
        } else {
          HandledErrorResult(event, this, dataService, configuration, json, logResultOption)
        }
      }
    }
  }

  def toRaw: RawBehaviorVersion = {
    RawBehaviorVersion(
      id,
      behavior.id,
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

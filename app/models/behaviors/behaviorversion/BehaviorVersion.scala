package models.behaviors.behaviorversion

import java.nio.ByteBuffer
import java.nio.charset.Charset

import models.accounts.user.User
import models.behaviors.events.MessageEvent
import models.behaviors._
import models.behaviors.behavior.Behavior
import models.behaviors.config.awsconfig.AWSConfig
import models.team.Team
import org.joda.time.DateTime
import play.api.libs.json.{JsValue, Json}
import play.api.Configuration
import services.AWSLambdaConstants._
import services.{AWSLambdaLogResult, AWSLambdaService, DataService}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class BehaviorVersion(
                            id: String,
                            behavior: Behavior,
                            maybeDescription: Option[String],
                            maybeShortName: Option[String],
                            maybeFunctionBody: Option[String],
                            maybeResponseTemplate: Option[String],
                            maybeAuthor: Option[User],
                            createdAt: DateTime
                          ) {

  val team: Team = behavior.team

  def isSkill: Boolean = {
    maybeFunctionBody.exists { body =>
      Option(body).exists(_.trim.nonEmpty)
    }
  }

  def editLinkFor(configuration: Configuration): String = {
    val baseUrl = configuration.getString("application.apiBaseUrl").get
    val path = controllers.routes.BehaviorEditorController.edit(behavior.id)
    s"$baseUrl$path"
  }

  def description: String = maybeDescription.getOrElse("")

  def functionBody: String = maybeFunctionBody.getOrElse("")

  def awsParamsFor(maybeAWSConfig: Option[AWSConfig]): Array[String] = {
    maybeAWSConfig.map(_ => Array("AWS")).getOrElse(Array())
  }

  def functionWithParams(params: Array[String]): String = {
    val definitionUserParamsString = if (params.isEmpty) {
      ""
    } else {
      s"""\n${params.map(ea => ea ++ ",").mkString("\n")}\n"""
    }
    val possibleEndOfParamsNewline = if (params.isEmpty) { "" } else { "\n" }
    s"""function($definitionUserParamsString$CONTEXT_PARAM$possibleEndOfParamsNewline) {
        |  $functionBody
        |}""".stripMargin
  }

  def functionName: String = id

  def resultFor(parametersWithValues: Seq[ParameterWithValue], event: MessageEvent, service: AWSLambdaService, dataService: DataService): Future[BehaviorResult] = {
    for {
      envVars <- dataService.environmentVariables.allFor(team)
      result <- service.invoke(this, parametersWithValues, envVars, event)
    } yield result
  }

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
                 configuration: Configuration
               ): BehaviorResult = {
    val bytes = payload.array
    val jsonString = new java.lang.String( bytes, Charset.forName("UTF-8") )
    val json = Json.parse(jsonString)
    val logResultOption = Some(logResult)
    (json \ "result").toOption.map { successResult =>
      SuccessResult(successResult, parametersWithValues, maybeResponseTemplate, logResultOption)
    }.getOrElse {
      if ((json \ NO_RESPONSE_KEY).toOption.exists(_.as[Boolean])) {
        NoResponseResult(logResultOption)
      } else {
        if (isUnhandledError(json)) {
          UnhandledErrorResult(this, configuration, logResultOption)
        } else if (json.toString == "null") {
          NoCallbackTriggeredResult(this, configuration)
        } else if (isSyntaxError(json)) {
          SyntaxErrorResult(this, configuration, json, logResultOption)
        } else {
          HandledErrorResult(this, configuration, json, logResultOption)
        }
      }
    }
  }

  def toRaw: RawBehaviorVersion = {
    RawBehaviorVersion(id, behavior.id, maybeDescription, maybeShortName, maybeFunctionBody, maybeResponseTemplate, maybeAuthor.map(_.id), createdAt)
  }

}

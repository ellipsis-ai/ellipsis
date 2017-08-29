package models.behaviors.testing

import models.behaviors.BotResult
import models.behaviors.behaviorparameter.BehaviorParameter
import models.behaviors.behaviorversion.BehaviorVersion
import models.behaviors.config.requiredsimpletokenapi.RequiredSimpleTokenApi
import play.api.libs.json._
import json.Formatting._

import utils.UploadFileSpec

case class ResultOutput(kind: String, fullText: String, files: Seq[UploadFileSpec])

case class InvocationTestReportOutput(
                                      missingInputNames: Seq[String],
                                      missingSimpleTokens: Seq[String],
                                      missingUserEnvVars: Set[String],
                                      result: Option[ResultOutput]
                                    )

case class InvocationTestReport(
                                 behaviorVersion: BehaviorVersion,
                                 maybeResult: Option[BotResult],
                                 missingParams: Seq[BehaviorParameter],
                                 missingSimpleTokens: Seq[RequiredSimpleTokenApi],
                                 missingUserEnvVars: Set[String]
                              ) {

  def json: JsValue = {
    val data = InvocationTestReportOutput(
      missingParams.map(_.name),
      missingSimpleTokens.map(_.api.name),
      missingUserEnvVars,
      maybeResult.map { r =>
        ResultOutput(r.resultType.toString, r.fullText, r.files)
      }
    )
    Json.toJson(data)
  }
}

package models.behaviors.testing

import models.behaviors.BotResult
import models.behaviors.behaviorparameter.BehaviorParameter
import models.behaviors.behaviorversion.BehaviorVersion
import models.behaviors.config.requiredsimpletokenapi.RequiredSimpleTokenApi
import play.api.libs.json._

case class ResultOutput(kind: String, fullText: String)

case class InvocationTestReportOutput(
                                      missingParamNames: Seq[String],
                                      missingSimpleTokens: Seq[String],
                                      missingUserEnvVars: Seq[String],
                                      result: Option[ResultOutput]
                                    )

case class InvocationTestReport(
                                 behaviorVersion: BehaviorVersion,
                                 maybeResult: Option[BotResult],
                                 missingParams: Seq[BehaviorParameter],
                                 missingSimpleTokens: Seq[RequiredSimpleTokenApi],
                                 missingUserEnvVars: Seq[String]
                              ) {

  implicit val resultOutputWrites = Json.writes[ResultOutput]
  implicit val testReportOutputWrites = Json.writes[InvocationTestReportOutput]

  def json: JsValue = {
    val data = InvocationTestReportOutput(
      missingParams.map(_.name),
      missingSimpleTokens.map(_.api.name),
      missingUserEnvVars,
      maybeResult.map { r =>
        ResultOutput(r.resultType.toString, r.fullText)
      }
    )
    Json.toJson(data)
  }
}

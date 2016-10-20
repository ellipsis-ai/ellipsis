package models.behaviors.testing

import models.behaviors.BotResult
import models.behaviors.behaviorparameter.BehaviorParameter
import models.behaviors.behaviorversion.BehaviorVersion
import play.api.libs.json._

case class ResultOutput(kind: String, fullText: String)

case class InvocationTestReportOutput(
                                      missingParamNames: Seq[String],
                                      result: Option[ResultOutput]
                                    )

case class InvocationTestReport(
                                behaviorVersion: BehaviorVersion,
                                maybeResult: Option[BotResult],
                                missingParams: Seq[BehaviorParameter]
                              ) {

  implicit val resultOutputWrites = Json.writes[ResultOutput]
  implicit val testReportOutputWrites = Json.writes[InvocationTestReportOutput]

  def json: JsValue = {
    val data = InvocationTestReportOutput(
      missingParams.map(_.name),
      maybeResult.map { r =>
        ResultOutput(r.resultType.toString, r.fullText)
      }
    )
    Json.toJson(data)
  }
}

package models.behaviors.testing

import models.behaviors.BotResult
import models.behaviors.behaviorversion.BehaviorVersion
import play.api.libs.json._

case class InvocationTestReportOutput(
                                      kind: String,
                                      fullText: String
                                    )

case class InvocationTestReport(
                                result: BotResult,
                                behaviorVersion: BehaviorVersion
                              ) {

  implicit val outputWrites = Json.writes[InvocationTestReportOutput]

  def json: JsValue = {
    val data = InvocationTestReportOutput(
      result.resultType.toString,
      result.fullText
    )
    Json.toJson(data)
  }
}

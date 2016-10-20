package models.behaviors.testing

import models.behaviors.BehaviorResponse
import models.behaviors.behaviorversion.BehaviorVersion
import play.api.libs.json._

case class TriggerTestReportOutput(
                                     message: String,
                                     activatedTrigger: Option[String],
                                     paramValues: Map[String, String]
                                     )

case class TriggerTestReport(
                               event: TestEvent,
                               behaviorVersion: BehaviorVersion,
                               maybeBehaviorResponse: Option[BehaviorResponse]
                               ) {

  val maybeActivatedTrigger = maybeBehaviorResponse.map(_.activatedTrigger)

  def paramValues: Map[String, String] = maybeBehaviorResponse.map { behaviorResponse =>
    behaviorResponse.parametersWithValues.flatMap { p =>
      p.maybeValue.map { v => (p.parameter.name, v.text) }
    }.toMap
  }.getOrElse(Map())

  implicit val outputWrites = Json.writes[TriggerTestReportOutput]

  def json: JsValue = {
    val data = TriggerTestReportOutput(
      event.context.fullMessageText,
      maybeActivatedTrigger.map(_.pattern),
      paramValues
    )
    Json.toJson(data)
  }
}

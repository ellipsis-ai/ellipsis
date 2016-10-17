package models.behaviors.testing

import models.behaviors.BehaviorResponse
import models.behaviors.behaviorversion.BehaviorVersion
import play.api.libs.functional.syntax._
import play.api.libs.json._

case class TriggerTestReportOutput(
                                     message: String,
                                     activatedTrigger: Option[String],
                                     paramValues: Map[String, Option[String]]
                                     )

case class TriggerTestReport(
                               event: TestEvent,
                               behaviorVersion: BehaviorVersion,
                               maybeBehaviorResponse: Option[BehaviorResponse]
                               ) {

  val maybeActivatedTrigger = maybeBehaviorResponse.map(_.activatedTrigger)

  def paramValues: Map[String, Option[String]] = maybeBehaviorResponse.map { behaviorResponse =>
    behaviorResponse.parametersWithValues.map { p =>
      (p.parameter.name, p.maybeValue.map(_.text))
    }.toMap
  }.getOrElse(Map())

  // Manual JSON writer so that None values become null rather than omitted
  implicit val outputWrites: Writes[TriggerTestReportOutput] = (
    (__ \ "message").write[String] and
      (__ \ "activatedTrigger").write[Option[String]] and
      (__ \ "paramValues").write[Map[String, Option[String]]]
  )(unlift(TriggerTestReportOutput.unapply))

  def json: JsValue = {
    val data = TriggerTestReportOutput(
      event.context.fullMessageText,
      maybeActivatedTrigger.map(_.pattern),
      paramValues
    )
    Json.toJson(data)
  }
}

package models.behaviors.testing

import models.behaviors.behaviorversion.BehaviorVersion
import models.behaviors.events.{RunEvent, TestEventContext}
import models.behaviors.scheduling.Scheduled

case class TestRunEvent(
                         eventContext: TestEventContext,
                         behaviorVersion: BehaviorVersion,
                         arguments: Map[String, String],
                         maybeScheduled: Option[Scheduled]
                       ) extends TestEvent with RunEvent {

  override type EC = TestEventContext

  val team = eventContext.team
  val user = eventContext.user

  val maybeMessageId: Option[String] = None

}

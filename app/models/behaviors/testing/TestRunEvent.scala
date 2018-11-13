package models.behaviors.testing

import models.behaviors.behaviorversion.BehaviorVersion
import models.behaviors.events.{RunEvent, TestEventContext}

case class TestRunEvent(
                         eventContext: TestEventContext,
                         behaviorVersion: BehaviorVersion,
                         arguments: Map[String, String]
                       ) extends TestEvent with RunEvent {

  override type EC = TestEventContext

  val team = eventContext.team
  val user = eventContext.user

  val maybeMessageIdForReaction: Option[String] = None

}

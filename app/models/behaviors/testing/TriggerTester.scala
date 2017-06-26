package models.behaviors.testing

import models.behaviors.BehaviorResponse
import models.behaviors.behaviorversion.BehaviorVersion
import services.DefaultServices

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class TriggerTester(services: DefaultServices) {

  def test(event: TestEvent, behaviorVersion: BehaviorVersion): Future[TriggerTestReport] = {
    for {
      maybeResponse <- BehaviorResponse.allFor(event, None, Some(behaviorVersion.behavior), services).map(_.headOption)
    } yield {
      TriggerTestReport(event, behaviorVersion, maybeResponse)
    }
  }

}

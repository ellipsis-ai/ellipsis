package models.behaviors.testing

import models.behaviors.behaviorversion.BehaviorVersion
import services.DataService

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class TriggerTester(
                          dataService: DataService
                        ) {

  def test(event: TestEvent, behaviorVersion: BehaviorVersion): Future[TriggerTestReport] = {
    for {
      maybeResponse <- dataService.behaviorResponses.allFor(event, None, Some(behaviorVersion.behavior)).map(_.headOption)
    } yield {
      TriggerTestReport(event, behaviorVersion, maybeResponse)
    }
  }

}

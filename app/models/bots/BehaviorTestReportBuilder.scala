package models.bots

import javax.inject._

import models.bots.triggers.MessageTriggerQueries
import services.AWSLambdaService
import slick.driver.PostgresDriver.api._

import scala.concurrent.ExecutionContext.Implicits.global

@Singleton
class BehaviorTestReportBuilder @Inject() (lambdaService: AWSLambdaService) {

  def buildFor(event: TestEvent, behaviorVersion: BehaviorVersion): DBIO[BehaviorTestReport] = {
    for {
      maybeResponse <- BehaviorResponse.chooseFor(event, None, Some(behaviorVersion.behavior))
      _ <- maybeResponse.map { behaviorResponse =>
        behaviorResponse.run(lambdaService)
      }.getOrElse(DBIO.successful(Unit))
    } yield {
      BehaviorTestReport(event, behaviorVersion, maybeResponse)
    }
  }

}

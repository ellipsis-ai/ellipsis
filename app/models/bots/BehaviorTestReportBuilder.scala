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
      triggers <- MessageTriggerQueries.allFor(behaviorVersion)
      maybeActivatedTrigger <- DBIO.successful(triggers.find(_.isActivatedBy(event)))
      maybeResponse <- maybeActivatedTrigger.map { trigger =>
        for {
          params <- BehaviorParameterQueries.allFor(trigger.behaviorVersion)
          response <- BehaviorResponse.buildFor(event, trigger.behaviorVersion, trigger.invocationParamsFor(event, params))
        } yield Some(response)
      }.getOrElse(DBIO.successful(None))
      _ <- maybeResponse.map { behaviorResponse =>
        behaviorResponse.run(lambdaService)
      }.getOrElse(DBIO.successful(Unit))
    } yield {
      BehaviorTestReport(event, behaviorVersion, maybeActivatedTrigger)
    }
  }

}

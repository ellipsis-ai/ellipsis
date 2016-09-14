package models.bots

import javax.inject._

import models.bots.behaviorversion.BehaviorVersion
import services.{AWSLambdaService, DataService}
import slick.driver.PostgresDriver.api._

import scala.concurrent.ExecutionContext.Implicits.global

@Singleton
class BehaviorTestReportBuilder @Inject() (
                                            lambdaService: AWSLambdaService,
                                            dataService: DataService
                                          ) {

  def buildFor(event: TestEvent, behaviorVersion: BehaviorVersion): DBIO[BehaviorTestReport] = {
    for {
      maybeResponse <- DBIO.from(BehaviorResponse.chooseFor(event, None, Some(behaviorVersion.behavior), dataService))
      _ <- maybeResponse.map { behaviorResponse =>
        behaviorResponse.result(lambdaService, dataService)
      }.getOrElse(DBIO.successful(Unit))
    } yield {
      BehaviorTestReport(event, behaviorVersion, maybeResponse)
    }
  }

}

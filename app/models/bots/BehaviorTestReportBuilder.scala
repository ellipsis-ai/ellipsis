package models.bots

import javax.inject._

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
      maybeResponse <- BehaviorResponse.chooseFor(event, None, Some(behaviorVersion.behavior))
      _ <- maybeResponse.map { behaviorResponse =>
        behaviorResponse.result(lambdaService, dataService)
      }.getOrElse(DBIO.successful(Unit))
    } yield {
      BehaviorTestReport(event, behaviorVersion, maybeResponse)
    }
  }

}

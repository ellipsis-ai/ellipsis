package models.behaviors

import javax.inject._

import models.behaviors.behaviorversion.BehaviorVersion
import play.api.libs.ws.WSClient
import services.{AWSLambdaService, DataService}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class BehaviorTestReportBuilder @Inject() (
                                            lambdaService: AWSLambdaService,
                                            dataService: DataService,
                                            ws: WSClient
                                          ) {

  def buildFor(event: TestEvent, behaviorVersion: BehaviorVersion): Future[BehaviorTestReport] = {
    for {
      maybeResponse <- BehaviorResponse.chooseFor(event, None, Some(behaviorVersion.behavior), dataService, lambdaService, ws)
      _ <- maybeResponse.map { behaviorResponse =>
        behaviorResponse.result
      }.getOrElse(Future.successful(Unit))
    } yield {
      BehaviorTestReport(event, behaviorVersion, maybeResponse)
    }
  }

}

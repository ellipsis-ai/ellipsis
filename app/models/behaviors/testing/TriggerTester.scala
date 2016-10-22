package models.behaviors.testing

import models.behaviors.behaviorversion.BehaviorVersion
import models.behaviors.BehaviorResponse
import play.api.Configuration
import play.api.cache.CacheApi
import play.api.libs.ws.WSClient
import services.{AWSLambdaService, DataService}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class TriggerTester(
                          lambdaService: AWSLambdaService,
                          dataService: DataService,
                          cache: CacheApi,
                          ws: WSClient,
                          configuration: Configuration
                        ) {

  def test(event: TestEvent, behaviorVersion: BehaviorVersion): Future[TriggerTestReport] = {
    for {
      maybeResponse <- BehaviorResponse.allFor(event, None, Some(behaviorVersion.behavior), lambdaService, dataService, cache, ws, configuration).map(_.headOption)
    } yield {
      TriggerTestReport(event, behaviorVersion, maybeResponse)
    }
  }

}

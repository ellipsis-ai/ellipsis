package models.behaviors.testing

import akka.actor.ActorSystem
import models.behaviors.BehaviorResponse
import models.behaviors.behaviorversion.BehaviorVersion
import play.api.Configuration
import play.api.libs.ws.WSClient
import services.{AWSLambdaService, CacheService, DataService}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class TriggerTester(
                          lambdaService: AWSLambdaService,
                          dataService: DataService,
                          cacheService: CacheService,
                          ws: WSClient,
                          configuration: Configuration,
                          actorSystem: ActorSystem
                        ) {

  def test(event: TestEvent, behaviorVersion: BehaviorVersion): Future[TriggerTestReport] = {
    for {
      maybeResponse <- BehaviorResponse.allFor(event, None, Some(behaviorVersion.behavior), lambdaService, dataService, cacheService, ws, configuration, actorSystem).map(_.headOption)
    } yield {
      TriggerTestReport(event, behaviorVersion, maybeResponse)
    }
  }

}

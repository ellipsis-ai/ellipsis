package models.behaviors.testing

import akka.actor.ActorSystem
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
                          configuration: Configuration,
                          actorSystem: ActorSystem
                        ) {

  def test(event: TestEvent, behaviorVersion: BehaviorVersion): Future[TriggerTestReport] = {
    for {
      maybeResponse <- BehaviorResponse.allFor(event, None, Some(behaviorVersion.behavior), lambdaService, dataService, cache, ws, configuration, actorSystem).map(_.headOption)
    } yield {
      TriggerTestReport(event, behaviorVersion, maybeResponse)
    }
  }

}

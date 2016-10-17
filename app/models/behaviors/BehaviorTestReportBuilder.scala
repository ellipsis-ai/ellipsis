package models.behaviors

import javax.inject._

import models.behaviors.behaviorversion.BehaviorVersion
import play.api.cache.CacheApi
import services.{AWSLambdaService, DataService}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class BehaviorTestReportBuilder @Inject() (
                                            lambdaService: AWSLambdaService,
                                            dataService: DataService,
                                            cache: CacheApi
                                          ) {

  def buildFor(event: TestEvent, behaviorVersion: BehaviorVersion): Future[BehaviorTestReport] = {
    for {
      maybeResponse <- BehaviorResponse.allFor(event, None, Some(behaviorVersion.behavior), lambdaService, dataService, cache).map(_.headOption)
// TODO: Disabled for now, because we want to separate testing the behavior code from testing the triggers
//      _ <- maybeResponse.map { behaviorResponse =>
//        behaviorResponse.result
//      }.getOrElse(Future.successful(Unit))
    } yield {
      BehaviorTestReport(event, behaviorVersion, maybeResponse)
    }
  }

}

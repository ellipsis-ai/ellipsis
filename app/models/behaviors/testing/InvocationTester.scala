package models.behaviors.testing

import models.behaviors.behaviorversion.BehaviorVersion
import models.behaviors.ParameterWithValue
import services.{AWSLambdaService, DataService}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class InvocationTester(
                            lambdaService: AWSLambdaService,
                            dataService: DataService
                          ) {

  def test(event: TestEvent, behaviorVersion: BehaviorVersion, parametersWithValues: Seq[ParameterWithValue]): Future[InvocationTestReport] = {
    dataService.behaviorVersions.resultFor(behaviorVersion, parametersWithValues, event).map { result =>
      InvocationTestReport(result, behaviorVersion)
    }
  }

}

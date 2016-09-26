package models.behaviors.behaviorparameter

import javax.inject.Inject

import com.google.inject.Provider
import models.team.Team
import services.DataService

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class BehaviorParameterTypeServiceImpl @Inject() (
                                                   dataServiceProvider: Provider[DataService]
                                                 ) extends BehaviorParameterTypeService {

  def dataService = dataServiceProvider.get

  def allFor(team: Team): Future[Seq[BehaviorParameterType]] = {
    for {
      dataTypes <- dataService.apiBackedDataTypes.allFor(team)
      paramTypes <- Future.sequence(dataTypes.map { dt =>
        ApiBackedBehaviorParameterType.buildFor(dt, dataService)
      })
    } yield {
      BehaviorParameterType.allBuiltIn ++ paramTypes
    }
  }

  def isValid(text: String, parameterType: BehaviorParameterType): Future[Boolean] = {
    Future.successful(true)
  }
}

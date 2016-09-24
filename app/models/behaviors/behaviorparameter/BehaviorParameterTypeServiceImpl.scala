package models.behaviors.behaviorparameter

import javax.inject.Inject

import com.google.inject.Provider
import json.{BehaviorParameterData, BehaviorParameterTypeData}
import models.IDs
import models.behaviors.behaviorversion.BehaviorVersion
import models.team.Team
import services.DataService
import slick.driver.PostgresDriver.api._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class BehaviorParameterTypeServiceImpl @Inject() (
                                                   dataServiceProvider: Provider[DataService]
                                                 ) extends BehaviorParameterTypeService {

  def dataService = dataServiceProvider.get

  def allFor(team: Team): Future[Seq[BehaviorParameterType]] = {
    dataService.apiBackedDataTypes.allFor(team).map { dataTypes =>
      BehaviorParameterType.allBuiltIn ++ dataTypes
    }
  }

  def isValid(text: String, parameterType: BehaviorParameterType): Future[Boolean] = {
    Future.successful(true)
  }
}

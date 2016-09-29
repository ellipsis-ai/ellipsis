package export

import json.BehaviorBackedDataTypeData
import models.team.Team
import models.accounts.user.User
import models.behaviors.behaviorparameter.BehaviorBackedDataType
import services.DataService

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class BehaviorBackedDataTypeImporter(
                                           team: Team,
                                           user: User,
                                           data: BehaviorBackedDataTypeData,
                                           dataService: DataService
                                        ) extends Importer[BehaviorBackedDataType] {

  def run: Future[BehaviorBackedDataType] = {
    for {
      behavior <- dataService.behaviors.createFor(team, None)
      version <- dataService.behaviorVersions.createFor(behavior, Some(user), data.behaviorVersionData(dataService))
      dataType <- dataService.behaviorBackedDataTypes.createFor(data.config.name, behavior)
    } yield dataType
  }

}

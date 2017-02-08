package export

import json.{BehaviorGroupData, BehaviorVersionData, InputData}
import models.team.Team
import models.accounts.user.User
import models.behaviors.behaviorgroup.BehaviorGroup
import models.behaviors.behaviorversion.BehaviorVersion
import models.behaviors.input.Input
import services.DataService

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class BehaviorGroupImporter(
                                   team: Team,
                                   user: User,
                                   data: BehaviorGroupData,
                                   dataService: DataService
                                 ) {

  def importBehaviorVersions(versions: Seq[BehaviorVersionData]): Future[Seq[Option[BehaviorVersion]]] = {
    Future.sequence(
      versions.map { versionData =>
        BehaviorVersionImporter(team, user, versionData, dataService).run
      }
    )
  }

  def importInputs(inputs: Seq[InputData], dataTypes: Seq[BehaviorVersion]): Future[Seq[Input]] = {
    Future.sequence(
      inputs.map { inputData =>
        val maybeOldDataTypeId = inputData.paramType.map(_.id)
        val maybeNewDataTypeId = maybeOldDataTypeId.flatMap(oldId => dataTypes.find(_.behavior.maybeImportedId.contains(oldId))).map(_.behavior.id)
        val withNewDataTypeId = maybeNewDataTypeId.map { newId =>
          inputData.copy(paramType = inputData.paramType.map(_.copy(id = newId)))
        }.getOrElse(inputData)
        dataService.inputs.ensureFor(withNewDataTypeId, team)
      }
    )
  }

  def run: Future[Option[BehaviorGroup]] = {

    dataService.behaviorGroups.createFor(data.name, data.description, data.publishedId, team).flatMap { group =>
      val behaviorVersionsWithGroupInfo = data.behaviorVersions.map { ea =>
        ea.copy(groupId = Some(group.id), importedId = data.publishedId)
      }
      val (dataTypesData, actionsData) = behaviorVersionsWithGroupInfo.partition(_.isDataType)
      for {
        dataTypes <- importBehaviorVersions(dataTypesData).map(_.flatten)
        _ <- importInputs(data.inputs, dataTypes)
        _ <- importBehaviorVersions(actionsData)
      } yield {
        Some(group)
      }

    }

  }

}

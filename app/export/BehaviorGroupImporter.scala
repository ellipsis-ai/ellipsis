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

  val inputExportIdToIdMapping = collection.mutable.Map[String, String]()

  def importBehaviorVersions(versions: Seq[BehaviorVersionData]): Future[Seq[Option[BehaviorVersion]]] = {
    Future.sequence(
      versions.map { versionData =>
        val paramsWithNewInputIds = versionData.params.map { param =>
          val maybeNewId = param.inputExportId.flatMap { exportId =>
            inputExportIdToIdMapping.get(exportId)
          }
          param.copy(inputId = maybeNewId, inputExportId = None)
        }
        BehaviorVersionImporter(team, user, versionData.copy(params = paramsWithNewInputIds), dataService).run
      }
    )
  }

  def importInputs(inputs: Seq[InputData], dataTypes: Seq[BehaviorVersion], group: BehaviorGroup): Future[Seq[Input]] = {
    Future.sequence(
      inputs.map { inputData =>
        val maybeOldDataTypeId = inputData.paramType.map(_.exportId)
        val maybeNewDataTypeId = maybeOldDataTypeId.flatMap(oldId => dataTypes.find(_.behavior.maybeImportedId.contains(oldId))).map(_.behavior.id)
        val withNewDataTypeId = maybeNewDataTypeId.map { newId =>
          inputData.copy(paramType = inputData.paramType.map(_.copy(id = Some(newId))))
        }.getOrElse(inputData)
        val withNewGroupId = if (inputData.groupId.isDefined) {
          withNewDataTypeId.copy(groupId = Some(group.id))
        } else {
          withNewDataTypeId
        }
        dataService.inputs.ensureFor(withNewGroupId, team).map { newInput =>
          inputData.exportId.foreach { exportId =>
            inputExportIdToIdMapping.put(exportId, newInput.id)
          }
          newInput
        }
      }
    )
  }

  def run: Future[Option[BehaviorGroup]] = {

    dataService.behaviorGroups.createFor(data.name, data.icon, data.description, data.publishedId, team).flatMap { group =>
      val behaviorVersionsWithGroupInfo = data.behaviorVersions.map { ea =>
        ea.copy(groupId = Some(group.id), importedId = data.publishedId)
      }
      val (dataTypesData, actionsData) = behaviorVersionsWithGroupInfo.partition(_.isDataType)
      for {
        dataTypes <- importBehaviorVersions(dataTypesData).map(_.flatten)
        _ <- importInputs(data.inputs, dataTypes, group)
        _ <- importBehaviorVersions(actionsData)
      } yield {
        Some(group)
      }

    }

  }

}

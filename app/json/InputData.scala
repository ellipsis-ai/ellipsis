package json

import export.BehaviorGroupExporter
import models.IDs
import models.behaviors.behaviorgroup.BehaviorGroup
import models.behaviors.input.Input
import services.DataService

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class InputData(
                      id: Option[String],
                      exportId: Option[String],
                      name: String,
                      paramType: Option[BehaviorParameterTypeData],
                      question: String,
                      isSavedForTeam: Boolean,
                      isSavedForUser: Boolean,
                      groupId: Option[String]
                    ) {

  val maybeNonEmptyQuestion: Option[String] = Option(question).filter(_.nonEmpty)

  def copyForExport(groupExporter: BehaviorGroupExporter): InputData = {
    copy(id = None, paramType = paramType.map(_.copyForExport(groupExporter)))
  }

  def copyWithIdsEnsuredFor(group: BehaviorGroup): InputData = {
    copy(
      groupId = Some(group.id),
      id = id.orElse(Some(IDs.next))
    )
  }

  def copyWithParamTypeIdsFrom(dataTypeVersions: Seq[BehaviorVersionData]): InputData = {
    val maybeOldDataTypeId = paramType.flatMap(_.exportId)
    val maybeNewDataTypeId = maybeOldDataTypeId.flatMap(oldId => dataTypeVersions.find(_.exportId.contains(oldId))).flatMap(_.behaviorId)
    maybeNewDataTypeId.map { newId =>
      copy(paramType = paramType.map(_.copy(id = Some(newId))))
    }.getOrElse(this)
  }

}

object InputData {

  def fromInput(input: Input, dataService: DataService): Future[InputData] = {
    BehaviorParameterTypeData.from(input.paramType, dataService).map { paramTypeData =>
      InputData(
        Some(input.id),
        input.maybeExportId,
        input.name,
        Some(paramTypeData),
        input.question,
        input.isSavedForTeam,
        input.isSavedForUser,
        input.maybeBehaviorGroup.map(_.id)
      )
    }

  }
}

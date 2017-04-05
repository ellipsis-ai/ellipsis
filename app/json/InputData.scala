package json

import export.BehaviorGroupExporter
import models.IDs
import models.behaviors.input.Input
import services.DataService

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class InputData(
                      id: Option[String],
                      inputId: Option[String],
                      exportId: Option[String],
                      name: String,
                      paramType: Option[BehaviorParameterTypeData],
                      question: String,
                      isSavedForTeam: Boolean,
                      isSavedForUser: Boolean
                    ) {

  val maybeNonEmptyQuestion: Option[String] = Option(question).filter(_.nonEmpty)

  def copyForExport(groupExporter: BehaviorGroupExporter): InputData = {
    copy(
      id = None,
      inputId = None,
      paramType = paramType.map(_.copyForExport(groupExporter))
    )
  }

  def copyWithIdsEnsuredFor(maybeExistingGroupData: Option[BehaviorGroupData]): InputData = {
    val maybeExisting = maybeExistingGroupData.flatMap { data =>
      data.inputs.find(_.exportId == exportId)
    }
    copy(
      id = id.orElse(Some(IDs.next)),
      inputId = maybeExisting.flatMap(_.inputId).orElse(inputId).orElse(Some(IDs.next))
    )
  }

  def copyWithParamTypeIdsIn(oldToNewIdMapping: collection.mutable.Map[String, String]): InputData = {
    val maybeOldDataTypeId = paramType.flatMap(_.id)
    val maybeNewDataTypeId = maybeOldDataTypeId.flatMap(oldId => oldToNewIdMapping.get(oldId))
    maybeNewDataTypeId.map { newId =>
      copy(paramType = paramType.map(_.copy(id = Some(newId))))
    }.getOrElse(this)
  }

  def copyWithParamTypeIdFromExportId(behaviorVersionsData: Seq[BehaviorVersionData]): InputData = {
    val maybeMatchingBehaviorVersion = behaviorVersionsData.find(_.exportId == paramType.flatMap(_.exportId))
    copy(paramType = paramType.map(_.copy(id = maybeMatchingBehaviorVersion.flatMap(_.id))))
  }

  def copyWithNewIdIn(oldToNewIdMapping: collection.mutable.Map[String, String]): InputData = {
    val newId = IDs.next
    val maybeOldID = id
    maybeOldID.foreach { oldId => oldToNewIdMapping.put(oldId, newId) }
    copy(id = Some(newId))
  }

}

object InputData {

  def fromInput(input: Input, dataService: DataService): Future[InputData] = {
    BehaviorParameterTypeData.from(input.paramType, dataService).map { paramTypeData =>
      InputData(
        Some(input.id),
        Some(input.inputId),
        input.maybeExportId,
        input.name,
        Some(paramTypeData),
        input.question,
        input.isSavedForTeam,
        input.isSavedForUser
      )
    }

  }

  def newUnsavedNamed(name: String, paramType: BehaviorParameterTypeData): InputData = InputData(
    id = Some(IDs.next),
    inputId = Some(IDs.next),
    exportId = None,
    name,
    Some(paramType),
    "",
    isSavedForTeam = false,
    isSavedForUser = false
  )
}

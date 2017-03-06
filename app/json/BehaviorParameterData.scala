package json

import export.BehaviorGroupExporter

case class BehaviorParameterData(
                                  name: String,
                                  paramType: Option[BehaviorParameterTypeData],
                                  question: String,
                                  isSavedForTeam: Option[Boolean],
                                  isSavedForUser: Option[Boolean],
                                  inputId: Option[String],
                                  inputExportId: Option[String],
                                  groupId: Option[String]
                                ) {

  val isShared = groupId.isDefined

  def newInputData = InputData(
    None,
    inputExportId,
    name,
    paramType,
    question,
    isSavedForTeam.exists(identity),
    isSavedForUser.exists(identity),
    groupId
  )

  def copyForExport(groupExporter: BehaviorGroupExporter): BehaviorParameterData = {
    copy(
      paramType = paramType.map(_.copyForExport(groupExporter)),
      inputId = inputId.flatMap(groupExporter.exportIdForInputId),
      groupId = groupId.flatMap { _ => groupExporter.behaviorGroupVersion.group.maybeExportId }
    )
  }


}

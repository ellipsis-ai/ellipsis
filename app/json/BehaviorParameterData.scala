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

  def copyForExport(groupExporter: BehaviorGroupExporter): BehaviorParameterData = {
    copy(
      paramType = paramType.map(_.copyForExport(groupExporter)),
      inputId = inputId.flatMap(groupExporter.exportIdForInputId),
      groupId = groupId.map { _ => groupExporter.behaviorGroup.publishedId }
    )
  }


}

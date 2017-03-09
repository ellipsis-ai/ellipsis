package json

import export.BehaviorGroupExporter

case class BehaviorParameterData(
                                  name: String,
                                  paramType: Option[BehaviorParameterTypeData],
                                  question: String,
                                  isSavedForTeam: Option[Boolean],
                                  isSavedForUser: Option[Boolean],
                                  inputId: Option[String],
                                  inputVersionId: Option[String],
                                  inputExportId: Option[String]
                                ) {

  lazy val isSaved: Boolean = isSavedForTeam.exists(identity) || isSavedForUser.exists(identity)

  def newInputData = InputData(
    None,
    inputId,
    inputExportId,
    name,
    paramType,
    question,
    isSavedForTeam.exists(identity),
    isSavedForUser.exists(identity)
  )

  def inputData = InputData(
    inputVersionId,
    inputId,
    inputExportId,
    name,
    paramType,
    question,
    isSavedForTeam.exists(identity),
    isSavedForUser.exists(identity)
  )

  def copyForExport(groupExporter: BehaviorGroupExporter): BehaviorParameterData = {
    copy(
      paramType = paramType.map(_.copyForExport(groupExporter)),
      inputId = inputId.flatMap(groupExporter.exportIdForInputId),
      inputVersionId = None
    )
  }


}

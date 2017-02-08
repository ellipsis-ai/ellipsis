package json

case class BehaviorParameterData(
                                  name: String,
                                  paramType: Option[BehaviorParameterTypeData],
                                  question: String,
                                  isSavedForTeam: Option[Boolean],
                                  isSavedForUser: Option[Boolean],
                                  inputId: Option[String],
                                  groupId: Option[String]
                                ) {

  val isShared = groupId.isDefined

  val inputData = InputData(
    inputId,
    name,
    paramType,
    question,
    isSavedForTeam.exists(identity),
    isSavedForUser.exists(identity),
    groupId
  )

  def copyWithIdMappings(dataTypeIdMapping: Map[String, String], inputIdMapping: Map[String, String]): BehaviorParameterData = {
    copy(
      paramType = paramType.map(_.copyWithIdMapping(dataTypeIdMapping)),
      inputId = inputId.flatMap { iid => inputIdMapping.get(iid) }
    )
  }


}

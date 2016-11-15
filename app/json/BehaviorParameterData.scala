package json

case class BehaviorParameterData(
                                  name: String,
                                  paramType: Option[BehaviorParameterTypeData],
                                  question: String,
                                  isSavedForTeam: Option[Boolean],
                                  isSavedForUser: Option[Boolean]
                                ) {

  val inputData = InputData(name, paramType, question, isSavedForTeam.exists(identity), isSavedForUser.exists(identity))

  def copyWithAttachedDataTypeFrom(dataTypes: Seq[BehaviorVersionData]): BehaviorParameterData = {
    copy(paramType = paramType.map(_.copyWithAttachedDataTypeFrom(dataTypes)))
  }

}

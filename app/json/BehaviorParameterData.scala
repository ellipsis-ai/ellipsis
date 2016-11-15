package json

case class BehaviorParameterData(name: String, paramType: Option[BehaviorParameterTypeData], question: String) {

  val inputData = InputData(name, paramType, question)

  def copyWithAttachedDataTypeFrom(dataTypes: Seq[BehaviorVersionData]): BehaviorParameterData = {
    copy(paramType = paramType.map(_.copyWithAttachedDataTypeFrom(dataTypes)))
  }

}

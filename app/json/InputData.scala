package json

case class InputData(name: String, paramType: Option[BehaviorParameterTypeData], question: String) {

  val maybeNonEmptyQuestion: Option[String] = Option(question).filter(_.nonEmpty)

  def copyWithAttachedDataTypeFrom(dataTypes: Seq[BehaviorVersionData]): InputData = {
    copy(paramType = paramType.map(_.copyWithAttachedDataTypeFrom(dataTypes)))
  }

}

package json

case class BehaviorParameterData(name: String, paramType: BehaviorParameterTypeData, question: String) {

  val maybeNonEmptyQuestion: Option[String] = Option(question).filter(_.nonEmpty)

}

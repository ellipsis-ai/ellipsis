package json

case class BehaviorParameterData(name: String, paramType: Option[BehaviorParameterTypeData], question: String) {

  val maybeNonEmptyQuestion: Option[String] = Option(question).filter(_.nonEmpty)

}

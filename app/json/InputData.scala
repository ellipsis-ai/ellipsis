package json

case class InputData(
                      name: String,
                      paramType: Option[BehaviorParameterTypeData],
                      question: String,
                      isSavedForTeam: Boolean,
                      isSavedForUser: Boolean
                    ) {

  val maybeNonEmptyQuestion: Option[String] = Option(question).filter(_.nonEmpty)

}

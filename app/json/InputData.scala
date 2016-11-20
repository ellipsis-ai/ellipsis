package json

case class InputData(
                      id: Option[String],
                      name: String,
                      paramType: Option[BehaviorParameterTypeData],
                      question: String,
                      isSavedForTeam: Boolean,
                      isSavedForUser: Boolean,
                      groupId: Option[String]
                    ) {

  val isShared = groupId.isDefined

  val maybeNonEmptyQuestion: Option[String] = Option(question).filter(_.nonEmpty)

}

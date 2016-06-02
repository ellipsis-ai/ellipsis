package utils

case class QuestionAnswerExtractor(messages: Seq[String]) {

  private def looksLikeQuestion(message: String): Boolean = {
    message.trim.endsWith("?")
  }

  lazy val maybeLastQuestionIndex: Option[Int] = {
    messages.reverse.zipWithIndex.
      find { case(ea, i) => looksLikeQuestion(ea) }.
      map { case(ea, i) => messages.length - 1 - i }
  }

  lazy val maybeLastQuestion: Option[String] = {
    maybeLastQuestionIndex.map(messages(_))
  }

  lazy val possibleAnswerContent: String = {
    val relevantMessages = maybeLastQuestionIndex.map { i =>
      messages.slice(i + 1, messages.length)
    }.getOrElse(messages)
    relevantMessages.mkString("\n")
  }
}

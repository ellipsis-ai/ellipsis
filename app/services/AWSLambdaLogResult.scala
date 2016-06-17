package services

case class AWSLambdaLogResult(source: String, userDefinedLogStatements: String, maybeError: Option[String])

object AWSLambdaLogResult {

  def extractErrorAndNonErrorContentFrom(text: String): (Option[String], String) = {
    var nonErrorContent = """(?s)(.*\n)END RequestId:""".r.findFirstMatchIn(text).flatMap { m =>
      m.subgroups.headOption
    }.getOrElse(text)
    var maybeErrorContent: Option[String] = None
    val extractErrorRegex = """(?s)(.*\n)\S+\t\S+\t(\S*Error:.*)\n[^\n]*\nEND.*""".r
    extractErrorRegex.findFirstMatchIn(text).foreach { m =>
      nonErrorContent = m.subgroups.head
      maybeErrorContent = m.subgroups.tail.headOption.map(s => s"```$s```")
    }
    (maybeErrorContent, nonErrorContent)
  }

  def extractUserDefinedLogStatementsFrom(text: String): String = {
    val maybeUserDefinedLogStatementsContent = """(?s)START.*?\n(.*)""".r.findFirstMatchIn(text).flatMap(_.subgroups.headOption)
    maybeUserDefinedLogStatementsContent.map { content =>
      content.split( """\S+\t\S+\t""")
    }.map { strings =>
      strings.
        map(_.trim).
        filter(_.nonEmpty)
        .map(s => s"You logged: ```$s```\n").
        mkString("")
    }.getOrElse("")
  }

  def fromText(text: String, isInDevelopmentMode: Boolean): AWSLambdaLogResult = {
    var (maybeErrorContent, nonErrorContent) = extractErrorAndNonErrorContentFrom(text)

    val userDefinedLogStatements = if (isInDevelopmentMode) {
      extractUserDefinedLogStatementsFrom(nonErrorContent)
    } else {
      ""
    }

    AWSLambdaLogResult(text, userDefinedLogStatements, maybeErrorContent)
  }

}

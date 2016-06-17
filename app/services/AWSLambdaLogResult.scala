package services

case class AWSLambdaLogResult(source: String, userDefinedLogStatements: String, maybeError: Option[String])

object AWSLambdaLogResult {

  def fromText(text: String, isInDevelopmentMode: Boolean): AWSLambdaLogResult = {
    var nonErrorContent = text
    var maybeErrorContent: Option[String] = None
    val extractErrorRegex = """(?s)(.*\n)\S+\t\S+\t(\S*Error:.*)\n[^\n]*\nEND.*""".r
    extractErrorRegex.findFirstMatchIn(text).foreach { m =>
      nonErrorContent = m.subgroups.head
      maybeErrorContent = m.subgroups.tail.headOption.map( s => s"```$s```" )
    }
    val maybeUserDefinedLogStatementsContent = """(?s)START.*?\n(.*)""".r.findFirstMatchIn(nonErrorContent).flatMap(_.subgroups.headOption)
    val userDefinedLogStatements = if (isInDevelopmentMode) {
      maybeUserDefinedLogStatementsContent.map { content =>
        content.split( """\S+\t\S+\t""")
      }.map { strings =>
        strings.
          map(_.trim).
          filter(_.nonEmpty)
          .map(s => s"You logged: ```$s```\n").
          mkString("")
      }.getOrElse("")
    } else {
      ""
    }

    AWSLambdaLogResult(text, userDefinedLogStatements, maybeErrorContent)
  }

}

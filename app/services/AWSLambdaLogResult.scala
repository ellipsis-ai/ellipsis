package services

case class AWSLambdaLogResult(source: String, userDefinedLogStatements: String, maybeError: Option[String]) {

  def shouldExcludeLine(line: String, functionLines: Int): Boolean = {
    """<your function>:(\d+):""".r.findFirstMatchIn(line).exists { m =>
      try {
        val lineNumber = m.subgroups.head.toInt
        lineNumber > functionLines
      } catch {
        case e: NumberFormatException => false
      }
    }
  }

  def maybeTranslated(functionLines: Int): Option[String] = {
    maybeError.map { error =>
      var translated = error
      translated = """/var/task/index.js""".r.replaceAllIn(translated, "<your function>")
      translated = """at fn|at exports\.handler""".r.replaceAllIn(translated, "at top level")
      translated.
        split("\n").
        filterNot { line => shouldExcludeLine(line, functionLines) }.
        mkString("\n").
        stripPrefix("\t")
    }
  }

}

object AWSLambdaLogResult {

  def extractErrorAndNonErrorContentFrom(text: String): (Option[String], String) = {
    var nonErrorContent = """(?s)(.*\n)END RequestId:""".r.findFirstMatchIn(text).flatMap { m =>
      m.subgroups.headOption
    }.getOrElse(text)
    var maybeErrorContent: Option[String] = None
    val syntaxErrorRegex = """(?s)(.*\n)(Syntax error in module 'index': SyntaxError.*)\n[^\n]*\nEND.*""".r
    syntaxErrorRegex.findFirstMatchIn(text).foreach { m =>
      nonErrorContent = m.subgroups.head
      maybeErrorContent = None
    }
    val extractErrorRegex = """(?s)(.*\n)\S+\t\S+\t(\S*Error:.*)\nEND.*""".r
    extractErrorRegex.findFirstMatchIn(text).foreach { m =>
      nonErrorContent = m.subgroups.head
      maybeErrorContent = m.subgroups.tail.headOption.map(s => s"\t$s")
    }
    val onErrorRegex = """(?s)(.*)[^\n]*\{\s*"errorMessage":"([^"]*).*""".r
    onErrorRegex.findFirstMatchIn(nonErrorContent).foreach { m =>
      nonErrorContent = m.subgroups.head
      val maybeOnErrorContent = m.subgroups.tail
    }
    (maybeErrorContent, nonErrorContent)
  }

  def extractUserDefinedLogStatementsFrom(text: String): String = {
    val maybeUserDefinedLogStatementsContent = """(?s)(START.*?\n)?(.*)""".r.findFirstMatchIn(text).flatMap(_.subgroups.tail.headOption)
    maybeUserDefinedLogStatementsContent.map { content =>
      content.split( """\S+\t\S+\t""")
    }.map { strings =>
      strings.
        map(_.trim).
        filter(_.nonEmpty).
        map(s => """\n""".r.replaceAllIn(s, "\n\t")).
        map(s => s"\nYou logged:\n\n\t$s\n").
        mkString("")
    }.getOrElse("")
  }

  def fromText(text: String): AWSLambdaLogResult = {
    val (maybeErrorContent, nonErrorContent) = extractErrorAndNonErrorContentFrom(text)
    val userDefinedLogStatements = extractUserDefinedLogStatementsFrom(nonErrorContent)

    AWSLambdaLogResult(text, userDefinedLogStatements, maybeErrorContent)
  }

  def empty: AWSLambdaLogResult = fromText("")

}

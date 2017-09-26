package services

import models.behaviors.behaviorversion.BehaviorVersion

case class AWSLambdaLogResult(source: String, authorDefinedLogStatements: String, maybeErrorMessage: Option[String]) {
  def maybeTranslated: Option[String] = {
    maybeErrorMessage.map(error => AWSLambdaLogResult.translateErrors(error))
  }
}

object AWSLambdaLogResult {

  def translateErrors(error: String): String = {
    var translated = error
    translated = s"""/var/task/${BehaviorVersion.dirName}/(.+)\\.js""".r.replaceAllIn(translated, "<your function>")
    translated = """/var/task/(.+)\.js""".r.replaceAllIn(translated, "$1")
    translated = """at fn|at exports\.handler""".r.replaceAllIn(translated, "at top level")
    translated.
      split("\n").
      mkString("\n").
      stripPrefix("\t")
  }

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
      content.split("""\S+\t\S+\t""")
    }.map { strings =>
      val logs = strings.map(_.trim).filter(_.nonEmpty)
      if (logs.nonEmpty) {
        logs.mkString("\n")
      } else {
        ""
      }
    }.getOrElse("")
  }

  def fromText(text: String): AWSLambdaLogResult = {
    val (maybeErrorContent, nonErrorContent) = extractErrorAndNonErrorContentFrom(text)
    val userDefinedLogStatements = extractUserDefinedLogStatementsFrom(nonErrorContent)

    AWSLambdaLogResult(text, userDefinedLogStatements, maybeErrorContent)
  }

  def empty: AWSLambdaLogResult = fromText("")

}

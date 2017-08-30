package services

case class AWSLambdaLogResult(source: String, authorDefinedLogStatements: String, maybeErrorMessage: Option[String], maybeUserErrorMessage: Option[String]) {

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
    maybeErrorMessage.map { error =>
      var translated = error
      translated = """/var/task/index.js""".r.replaceAllIn(translated, "<your function>")
      translated = """/var/task/(.+)\.js""".r.replaceAllIn(translated, "$1")
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

  private val stackTraceRegex = """(?s)(.+)\nELLIPSIS_STACK_TRACE_START\n(.+)ELLIPSIS_STACK_TRACE_END\Z""".r
  private val stackTraceSourceRegex = """^\s+at (.+?) \(/.+/(.+?)\.js:(\d+):(\d+)\)""".r
  private val userErrorRegex = """(?s)(.+)ELLIPSIS_USER_ERROR_MESSAGE_START\n(.+)ELLIPSIS_USER_ERROR_MESSAGE_END\n(.+)""".r

  def getLogStatementPrefix(ellipsisStackTrace: String): String = {
    val lines = ellipsisStackTrace.split("\n")
    val maybeLogMethod = lines.headOption.map {
      case "log" => ""
      case "info" => ""
      case "warn" => "⚠️" // intellij hides the warning emoji
      case "error" => "⛔️" // intellij hides the forbidden emoji
    }.filter(_.nonEmpty)
    val maybeSourceLine = lines.slice(1, 2).headOption
    maybeSourceLine.map {
      case stackTraceSourceRegex(funcName, sourceFile, lineNumber, charNumber) => {
        val lineInfo = Option(sourceFile).filterNot(_ == "index").map { sourceName =>
          s"$sourceName:$lineNumber"
        }.getOrElse(lineNumber)
        s"${maybeLogMethod.getOrElse("")}$lineInfo: "
      }
      case _ => ""
    }.filter(_.nonEmpty).getOrElse {
      maybeLogMethod.map(_ + ": ").getOrElse("")
    }
  }

  def extractUserErrorFrom(maybeText: Option[String]): (Option[String], Option[String]) = {
    val maybeErrorTuple: Option[(Option[String], Option[String])] = maybeText.map {
      case userErrorRegex(systemErrorMessage, userErrorMessage, stackTrace) =>
        (Some(systemErrorMessage + stackTrace), Some(userErrorMessage))
      case s: String =>
        (Some(s), None)
    }
    maybeErrorTuple.getOrElse {
      (None, None)
    }
  }

  def processAuthorLogs(logs: Seq[String]): Seq[String] = {
    logs.map {
      case stackTraceRegex(userContent, stackTrace) => getLogStatementPrefix(stackTrace) + userContent
      case s => s
    }
  }

  def extractUserDefinedLogStatementsFrom(text: String): String = {
    val maybeUserDefinedLogStatementsContent = """(?s)(START.*?\n)?(.*)""".r.findFirstMatchIn(text).flatMap(_.subgroups.tail.headOption)
    maybeUserDefinedLogStatementsContent.map { content =>
      content.split("""\S+\t\S+\t""")
    }.map { strings =>
      val logs = strings.map(_.trim).filter(_.nonEmpty)
      val processed = processAuthorLogs(logs)
      if (logs.nonEmpty) {
        processed.mkString("\n")
      } else {
        ""
      }
    }.getOrElse("")
  }

  def fromText(text: String): AWSLambdaLogResult = {
    val (maybeErrorContent, nonErrorContent) = extractErrorAndNonErrorContentFrom(text)
    val userDefinedLogStatements = extractUserDefinedLogStatementsFrom(nonErrorContent)
    val (maybeSystemError, maybeUserError) = extractUserErrorFrom(maybeErrorContent)

    AWSLambdaLogResult(text, userDefinedLogStatements, maybeSystemError, maybeUserError)
  }

  def empty: AWSLambdaLogResult = fromText("")

}

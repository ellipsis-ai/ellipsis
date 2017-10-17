package models.behaviors

case class ExecutionLogData(logged: String, stack: String) {
  private val stackTraceSourceRegex = """^\s+at\s+(?:\S*)\s*\(?/var/task/(.+)\.js:(\d+):(\d+)\)?""".r

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
      case stackTraceSourceRegex(sourceFile, lineNumber, charNumber) => {
        val lineInfo = Option(sourceFile).filterNot(_.contains("behavior_versions")).map { sourceName =>
          s"$sourceName:$lineNumber"
        }.getOrElse(lineNumber)
        s"${maybeLogMethod.getOrElse("")}$lineInfo: "
      }
      case _ => ""
    }.filter(_.nonEmpty).getOrElse {
      maybeLogMethod.map(_ + ": ").getOrElse("")
    }
  }

  override def toString: String = getLogStatementPrefix(stack) + logged
}

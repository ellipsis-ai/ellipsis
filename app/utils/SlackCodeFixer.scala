package utils

object SlackCodeFixer {

  def runFor(code: String): String = {
    var fixedCode = "[“”]".r.replaceAllIn(code, "\"")
    "‘".r.replaceAllIn(fixedCode, "'")
  }

}

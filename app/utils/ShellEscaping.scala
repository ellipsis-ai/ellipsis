package utils

object ShellEscaping {

  def escapeWithSingleQuotes(text: String): String = {
    "'" ++ text.replaceAll("""'""", """'\\''""") ++ "'"
  }

}

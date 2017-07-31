package models.behaviors.defaultstorageitem

object GraphQLHelpers {

  def formatTypeName(name: String): String = {
    val withValidFirstChar = """$[^_a-zA-Z]*""".r.replaceAllIn(name, "")
    val withValidChars = """[^_a-zA-Z0-9\s]""".r.replaceAllIn(withValidFirstChar, "")
    val parts: Array[String] = if (withValidChars.isEmpty) {
      Array()
    } else {
      withValidChars.split("""\s+""").map(ea => ea.charAt(0).toUpper.toString ++ ea.substring(1))
    }
    parts.mkString("")
  }

  def formatFieldName(name: String): String = {
    val capitalized = formatTypeName(name)
    capitalized.charAt(0).toLower.toString ++ capitalized.substring(1)
  }

  val fallbackTypeName: String = "UnnamedType"

}

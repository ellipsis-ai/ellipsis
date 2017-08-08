package utils

object NameFormatter {

  def formatDataTypeName(name: String): String = {
    val withCapitalizedFirstLetter = """^[^a-zA-Z]*""".r.replaceAllIn(name, "").capitalize
    """[^_0-9A-Za-z]""".r.replaceAllIn(withCapitalizedFirstLetter, "")
  }

}

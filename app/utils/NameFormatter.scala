package utils

import org.apache.commons.lang.WordUtils

object NameFormatter {

  def formatDataTypeName(name: String): String = {
    val withCapitalizedFirstLetter = """^[^a-zA-Z]*""".r.replaceAllIn(name, "").capitalize
    """[^_0-9A-Za-z]""".r.replaceAllIn(withCapitalizedFirstLetter, "")
  }

  def formatConfigPropertyName(name: String): String = {
    val words = name.split(" ").map((ea) => ea.replaceAll("""[^\w$]""", ""))
    val firstWord = WordUtils.uncapitalize(words.head)
    val camel = firstWord + words.tail.map((ea) => WordUtils.capitalize(ea)).mkString("")
    if (camel.head.toString.matches("""[A-Za-z_$]""")) {
      camel
    } else {
      "_" + camel
    }
  }

}

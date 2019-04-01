package utils

import scala.util.matching.Regex

object MSTeamsUtils {

  val emojiRegex = new Regex("""<img.+?alt=\"(.+?)\".+?>""", "altText")
  val nbspRegex = s"""&nbsp;"""

}

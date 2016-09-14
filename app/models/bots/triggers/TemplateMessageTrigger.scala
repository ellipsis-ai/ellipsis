package models.bots.triggers

import models.bots.behaviorparameter.BehaviorParameter
import models.bots.behaviorversion.BehaviorVersion
import models.bots.triggers.messagetrigger.MessageTrigger

import scala.util.matching.Regex

case class TemplateMessageTrigger(
                                id: String,
                                behaviorVersion: BehaviorVersion,
                                template: String,
                                requiresBotMention: Boolean,
                                isCaseSensitive: Boolean
                                ) extends MessageTrigger {

  val shouldTreatAsRegex: Boolean = false

  val sortRank: Int = 1

  val pattern: String = template

  def regex: Regex = {
    var pattern = template
    pattern = TemplateMessageTriggerUtils.escapeRegexCharactersIn(pattern)
    pattern = """\{.*?\}""".r.replaceAllIn(pattern, """(.+)""")
    pattern = """\s+""".r.replaceAllIn(pattern, """\\s+""")
    pattern = """[“”\"]""".r.replaceAllIn(pattern, """[“”\"]""")
    pattern = """[‘’']""".r.replaceAllIn(pattern, """[‘’']""")
    pattern = "^" ++ pattern
    if (!isCaseSensitive) {
      pattern = "(?i)" ++ pattern
    }
    pattern.r
  }

  private val templateParamNames: Seq[String] = {
    """\{(.*?)\}""".r.findAllMatchIn(template).flatMap(_.subgroups).toSeq
  }

  protected def paramIndexMaybesFor(params: Seq[BehaviorParameter]): Seq[Option[Int]] = {
    templateParamNames.map { paramName =>
      params.find(_.name == paramName).map(_.rank - 1)
    }
  }

}

object TemplateMessageTriggerUtils {

  // need to deal with \\ first
  val specialCharacters = Seq("\\", "$", "?", "-", "[", "]")

  def escapeRegexCharactersIn(text: String): String = {
    specialCharacters.foldLeft(text)((str, char) => str.replace(char, s"\\$char"))
  }
}

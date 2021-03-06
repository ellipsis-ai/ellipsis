package models.behaviors.triggers

import models.behaviors.behaviorparameter.BehaviorParameter
import models.behaviors.behaviorversion.BehaviorVersion

import scala.util.matching.Regex

case class TemplateTrigger(
                                id: String,
                                behaviorVersion: BehaviorVersion,
                                triggerType: TriggerType,
                                template: String,
                                requiresBotMention: Boolean,
                                isCaseSensitive: Boolean
                                ) extends Trigger {

  val shouldTreatAsRegex: Boolean = false

  val pattern: String = template

  val trimmedPattern: String = template.trim

  val paramRegex: Regex = """\{.*?\}""".r

  override val maybePattern: Option[String] = Some(paramRegex.replaceAllIn(trimmedPattern, ""))

  def regex: Regex = {
    var pattern = trimmedPattern
    pattern = TemplateTriggerUtils.escapeRegexCharactersIn(pattern)
    pattern = paramRegex.replaceAllIn(pattern, """(.+)""")
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

object TemplateTriggerUtils {

  // need to deal with \\ first
  val specialCharacters = Seq("\\", "^", "$", "?", "-", "[", "]", "(", ")", "+", "*", ".")

  def escapeRegexCharactersIn(text: String): String = {
    specialCharacters.foldLeft(text)((str, char) => str.replace(char, s"\\$char"))
  }
}

package models.bots.triggers

import java.util.regex.Matcher

import models.bots._
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
    pattern = Matcher.quoteReplacement(pattern)
    pattern = """\{.*?\}""".r.replaceAllIn(pattern, """(\\S+)""")
    pattern = """\s+""".r.replaceAllIn(pattern, """\\s+""")
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

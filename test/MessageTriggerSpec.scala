import models.bots.triggers.MessageTrigger
import org.scalatestplus.play.PlaySpec

import scala.util.matching.Regex

trait MessageTriggerSpec extends PlaySpec {

  val mentionRegex: Regex = s"""^<@1234>:?\\s*""".r

  def matchParamsFor(text: String): (String, String, Regex) = {
    (text, text, mentionRegex)
  }

  def matches(trigger: MessageTrigger, text: String): Boolean = {
    (trigger.matches _).tupled(matchParamsFor(text))
  }

}

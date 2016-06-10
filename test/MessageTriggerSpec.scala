import models.bots.triggers.MessageTrigger
import org.scalatestplus.play.PlaySpec

trait MessageTriggerSpec extends PlaySpec {

  def matches(trigger: MessageTrigger, text: String, includesBotMention: Boolean = false): Boolean = {
    trigger.matches(text, includesBotMention)
  }

}

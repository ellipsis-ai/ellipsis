import models.IDs
import models.behaviors.behaviorparameter.{BehaviorParameter, TextType}
import models.behaviors.behaviorversion.BehaviorVersion
import models.behaviors.input.Input
import models.behaviors.triggers.messagetrigger.MessageTrigger
import org.scalatestplus.play.PlaySpec

trait MessageTriggerSpec extends PlaySpec {

  def matches(trigger: MessageTrigger, text: String, includesBotMention: Boolean = false): Boolean = {
    trigger.matches(text, includesBotMention)
  }

  def newParameterFor(
                    name: String,
                    rank: Int,
                    behaviorVersion: BehaviorVersion
                  ): BehaviorParameter = {
    BehaviorParameter(IDs.next, rank, Input(IDs.next, name, None, TextType, false, false, None), behaviorVersion)
  }

}

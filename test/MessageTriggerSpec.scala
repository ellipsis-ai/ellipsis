import models.IDs
import models.bots.behaviorparameter.{BehaviorParameter, TextType}
import models.bots.behaviorversion.BehaviorVersion
import models.bots.triggers.messagetrigger.MessageTrigger
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
    BehaviorParameter(IDs.next, name, rank, behaviorVersion, None, new TextType())
  }

}

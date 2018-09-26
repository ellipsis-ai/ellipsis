package json

import models.behaviors.triggers.TriggerType

case class TriggerTypeData(
                            id: String,
                            displayString: String
                          )

object TriggerTypeData {

  def from(triggerType: TriggerType): TriggerTypeData = {
    TriggerTypeData(
      triggerType.toString,
      triggerType.displayString
    )
  }

}

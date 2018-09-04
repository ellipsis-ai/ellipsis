package json

import models.behaviors.behaviorversion.{BehaviorResponseType, Normal}

case class BehaviorResponseTypeData(id: String, displayString: String)

object BehaviorResponseTypeData {

  def from(responseType: BehaviorResponseType): BehaviorResponseTypeData = {
    BehaviorResponseTypeData(responseType.toString, responseType.displayName)
  }

  def normal: BehaviorResponseTypeData = from(Normal)

  def all: Seq[BehaviorResponseTypeData] = BehaviorResponseType.values.map(from)
}

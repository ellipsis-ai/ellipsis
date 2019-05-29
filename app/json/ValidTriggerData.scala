package json

case class ValidTriggerData(text: String, matchingTriggers: Seq[BehaviorTriggerData], matchingBehaviorIds: Seq[String])

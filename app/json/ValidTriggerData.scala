package json

case class ValidBehaviorIdTriggerData(behaviorId: String, triggers: Seq[BehaviorTriggerData])
case class ValidTriggerData(text: String, matchingBehaviorTriggers: Seq[ValidBehaviorIdTriggerData])

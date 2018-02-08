package models.loggedevent

import utils.Enum

sealed trait ResultType {
  val name: String
  override def toString: String = name
}


object ResultType extends Enum[ResultType] {
  val values = List(BehaviorRun, BotDidNotUnderstand, BehaviorIdNotFound, Nothing)
  def definitelyFind(name: String): ResultType = find(name).get
}

object BehaviorRun extends ResultType {
  val name: String = "behavior_run"
}

object BotDidNotUnderstand extends ResultType {
  val name: String = "bot_did_not_understand"
}

object BehaviorIdNotFound extends ResultType {
  val name: String = "behavior_id_not_found"
}

object Nothing extends ResultType {
  val name: String = "nothing"
}

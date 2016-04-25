package models.bots

case class BehaviorResponse(behavior: Behavior, event: Event, params: Map[String, String]) {
  def run: Unit = {
    event.context.sendMessage(behavior.resultFor(params))
  }
}

package models.bots

import services.AWSLambdaService

case class BehaviorResponse(behavior: Behavior, event: Event, params: Map[String, String]) {
  def run(service: AWSLambdaService): Unit = {
    event.context.sendMessage(behavior.resultFor(params, service))
  }
}

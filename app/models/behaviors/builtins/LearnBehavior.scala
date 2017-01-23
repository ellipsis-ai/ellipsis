package models.behaviors.builtins

import models.behaviors.events.MessageEvent
import models.behaviors.{BotResult, SimpleTextResult}
import services.{AWSLambdaService, DataService}

import scala.concurrent.Future

case class LearnBehavior(
                          event: MessageEvent,
                          lambdaService: AWSLambdaService,
                          dataService: DataService
                        ) extends BuiltinBehavior {

  def result: Future[BotResult] = {
    Future.successful(SimpleTextResult(event, s"I love to learn. Come ${event.teachMeLinkFor(lambdaService)}.", forcePrivateResponse = false))
  }

}

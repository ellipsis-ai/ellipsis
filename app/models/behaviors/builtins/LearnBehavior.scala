package models.behaviors.builtins

import models.behaviors.{BotResult, SimpleTextResult}
import services.slack.NewMessageEvent
import services.{AWSLambdaService, DataService}

import scala.concurrent.Future

case class LearnBehavior(
                          event: NewMessageEvent,
                          lambdaService: AWSLambdaService,
                          dataService: DataService
                        ) extends BuiltinBehavior {

  def result: Future[BotResult] = {
    Future.successful(SimpleTextResult(s"I love to learn. Come ${event.teachMeLinkFor(lambdaService)}.", forcePrivateResponse = false))
  }

}

package models.behaviors.builtins

import models.behaviors.{BotResult, SimpleTextResult}
import models.behaviors.events.MessageContext
import services.{AWSLambdaService, DataService}

import scala.concurrent.Future

case class LearnBehavior(
                          messageContext: MessageContext,
                          lambdaService: AWSLambdaService,
                          dataService: DataService
                        ) extends BuiltinBehavior {

  def result: Future[BotResult] = {
    Future.successful(SimpleTextResult(s"I love to learn. Come ${messageContext.teachMeLinkFor(lambdaService)}.", forcePrivateResponse = false))
  }

}

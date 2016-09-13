package models.bots.builtins

import models.bots.{BehaviorResult, SimpleTextResult}
import models.bots.events.MessageContext
import services.{AWSLambdaService, DataService}

import scala.concurrent.Future

case class LearnBehavior(
                          messageContext: MessageContext,
                          lambdaService: AWSLambdaService,
                          dataService: DataService
                        ) extends BuiltinBehavior {

  def result: Future[BehaviorResult] = {
    Future.successful(SimpleTextResult(s"I love to learn. Come ${messageContext.teachMeLinkFor(lambdaService)}."))
  }

}

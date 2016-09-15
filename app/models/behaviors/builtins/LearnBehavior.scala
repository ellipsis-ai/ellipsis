package models.behaviors.builtins

import models.behaviors.{BehaviorResult, SimpleTextResult}
import models.behaviors.events.MessageContext
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

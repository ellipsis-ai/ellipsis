package models.bots.builtins

import models.bots.{BehaviorResult, SimpleTextResult}
import models.bots.events.MessageContext
import services.AWSLambdaService
import slick.driver.PostgresDriver.api._

case class LearnBehavior(messageContext: MessageContext, lambdaService: AWSLambdaService) extends BuiltinBehavior {

  def result: DBIO[BehaviorResult] = {
    DBIO.successful(SimpleTextResult(s"I love to learn. Come ${messageContext.teachMeLinkFor(lambdaService)}."))
  }

}

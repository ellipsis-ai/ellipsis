package models.bots.builtins

import models.bots.MessageContext
import services.AWSLambdaService
import slick.driver.PostgresDriver.api._
import scala.concurrent.ExecutionContext.Implicits.global

case class LearnBehavior(messageContext: MessageContext, lambdaService: AWSLambdaService) extends BuiltinBehavior {

  def run: DBIO[Unit] = {
    messageContext.sendMessage(s"I love to learn. Come ${messageContext.teachMeLinkFor(lambdaService)}.")
    DBIO.successful(Unit)
  }

}

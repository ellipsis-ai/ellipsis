package models.bots

import models.bots.conversations.InvokeBehaviorConversation
import services.AWSLambdaService
import slick.dbio.DBIO
import scala.concurrent.ExecutionContext.Implicits.global

case class BehaviorResponse(
                             event: Event,
                             behavior: Behavior,
                             parameters: Seq[BehaviorParameter],
                             paramValues: Map[String, String]
                             ) {

  def isFilledOut: Boolean = {
    parameters.size == paramValues.size
  }

  def runCode(service: AWSLambdaService): Unit = event.context.sendMessage(behavior.resultFor(paramValues, service))

  def run(service: AWSLambdaService): DBIO[Unit] = {
    if (isFilledOut) {
      DBIO.successful(runCode(service))
    } else {
      for {
        convo <- InvokeBehaviorConversation.createFor(behavior, event.context.name, event.context.userIdForContext)
        _ <- convo.replyFor(event, service)
      } yield Unit
    }
  }
}

object BehaviorResponse {

  def buildFor(event: Event, behavior: Behavior, paramValues: Map[String, String]): DBIO[BehaviorResponse] = {
    BehaviorParameterQueries.allFor(behavior).map { params =>
      BehaviorResponse(event, behavior, params, paramValues)
    }
  }
}

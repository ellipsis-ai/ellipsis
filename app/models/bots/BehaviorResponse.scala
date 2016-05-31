package models.bots

import models.bots.conversations.InvokeBehaviorConversation
import services.AWSLambdaService
import slick.dbio.DBIO
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class ParameterWithValue(parameter: BehaviorParameter, invocationName: String, maybeValue: Option[String]) {
  def value: String = maybeValue.getOrElse("")
}

case class BehaviorResponse(
                             event: Event,
                             behavior: Behavior,
                             parametersWithValues: Seq[ParameterWithValue]
                             ) {

  def isFilledOut: Boolean = {
    parametersWithValues.forall(_.maybeValue.isDefined)
  }

  def runCode(service: AWSLambdaService): Future[Unit] = {
    behavior.resultFor(parametersWithValues, service).map { result =>
      event.context.sendMessage(result)
    }
  }

  def run(service: AWSLambdaService): DBIO[Unit] = {
    if (isFilledOut) {
      DBIO.from(runCode(service))
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
      val paramsWithValues = params.zipWithIndex.map { case (ea, i) =>
        val invocationName = s"param$i"
        ParameterWithValue(ea, invocationName, paramValues.get(invocationName))
      }
      BehaviorResponse(event, behavior, paramsWithValues)
    }
  }
}

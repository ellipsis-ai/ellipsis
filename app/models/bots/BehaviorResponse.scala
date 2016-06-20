package models.bots

import models.Team
import models.bots.conversations.{CollectedParameterValue, InvokeBehaviorConversation}
import models.bots.triggers.MessageTriggerQueries
import services.{AWSLambdaConstants, AWSLambdaService}
import slick.dbio.DBIO
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class ParameterWithValue(parameter: BehaviorParameter, invocationName: String, maybeValue: Option[String]) {
  def value: String = maybeValue.getOrElse("")
}

case class BehaviorResponse(
                             event: Event,
                             behaviorVersion: BehaviorVersion,
                             parametersWithValues: Seq[ParameterWithValue]
                             ) {

  def isFilledOut: Boolean = {
    parametersWithValues.forall(_.maybeValue.isDefined)
  }

  def runCode(service: AWSLambdaService): Future[Unit] = {
    behaviorVersion.unformattedResultFor(parametersWithValues, service).map { result =>
      event.context.sendMessage(result)
    }
  }

  def run(service: AWSLambdaService): DBIO[Unit] = {
    if (isFilledOut) {
      DBIO.from(runCode(service))
    } else {
      for {
        convo <- InvokeBehaviorConversation.createFor(behaviorVersion, event.context.name, event.context.userIdForContext)
        _ <- DBIO.sequence(parametersWithValues.map { p =>
          p.maybeValue.map { v =>
            CollectedParameterValue(p.parameter, convo, v).save
          }.getOrElse(DBIO.successful(Unit))
        })
        _ <- convo.replyFor(event, service)
      } yield Unit
    }
  }
}

object BehaviorResponse {

  def buildFor(event: Event, behaviorVersion: BehaviorVersion, paramValues: Map[String, String]): DBIO[BehaviorResponse] = {
    BehaviorParameterQueries.allFor(behaviorVersion).map { params =>
      val paramsWithValues = params.zipWithIndex.map { case (ea, i) =>
        val invocationName = AWSLambdaConstants.invocationParamFor(i)
        ParameterWithValue(ea, invocationName, paramValues.get(invocationName))
      }
      BehaviorResponse(event, behaviorVersion, paramsWithValues)
    }
  }

  def allFor(event: Event, team: Team): DBIO[Seq[BehaviorResponse]] = {
    for {
      triggers <- MessageTriggerQueries.allActiveFor(team)
      activated <- DBIO.successful(triggers.filter(_.isActivatedBy(event)))
      responses <- DBIO.sequence(activated.map { trigger =>
        for {
          params <- BehaviorParameterQueries.allFor(trigger.behaviorVersion)
          response <- BehaviorResponse.buildFor(event, trigger.behaviorVersion, trigger.invocationParamsFor(event, params))
        } yield response
      })
    } yield responses
  }
}

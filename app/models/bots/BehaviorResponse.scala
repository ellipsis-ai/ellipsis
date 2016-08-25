package models.bots

import models.Team
import models.bots.conversations.{CollectedParameterValue, InvokeBehaviorConversation}
import models.bots.events.MessageEvent
import models.bots.triggers.{MessageTrigger, MessageTriggerQueries}
import org.joda.time.DateTime
import services.{AWSLambdaConstants, AWSLambdaService}
import slick.dbio.DBIO

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class ParameterWithValue(parameter: BehaviorParameter, invocationName: String, maybeValue: Option[String]) {
  def value: String = maybeValue.getOrElse("")
}

case class BehaviorResponse(
                             event: MessageEvent,
                             behaviorVersion: BehaviorVersion,
                             parametersWithValues: Seq[ParameterWithValue],
                             activatedTrigger: MessageTrigger
                             ) {

  def isFilledOut: Boolean = {
    parametersWithValues.forall(_.maybeValue.isDefined)
  }

  def runCode(service: AWSLambdaService): Future[BehaviorResult] = {
    val startTime = DateTime.now
    behaviorVersion.resultFor(parametersWithValues, event, service).flatMap { result =>
      val runtimeInMilliseconds = DateTime.now.toDate.getTime - startTime.toDate.getTime
      result.sendIn(event.context)
      service.models.run(
        InvocationLogEntryQueries.createFor(
          behaviorVersion,
          result,
          event.context.name,
          Some(event.context.userIdForContext),
          runtimeInMilliseconds
        ).map(_ => result)
      )
    }
  }

  def run(service: AWSLambdaService): DBIO[BehaviorResult] = {
    if (isFilledOut) {
      DBIO.from(runCode(service))
    } else {
      for {
        convo <- InvokeBehaviorConversation.createFor(behaviorVersion, event.context.name, event.context.userIdForContext, activatedTrigger)
        _ <- DBIO.sequence(parametersWithValues.map { p =>
          p.maybeValue.map { v =>
            CollectedParameterValue(p.parameter, convo, v).save
          }.getOrElse(DBIO.successful(Unit))
        })
        result <- convo.replyFor(event, service)
      } yield result
    }
  }
}

object BehaviorResponse {

  def buildFor(
                event: MessageEvent,
                behaviorVersion: BehaviorVersion,
                paramValues: Map[String, String],
                activatedTrigger: MessageTrigger
                ): DBIO[BehaviorResponse] = {
    BehaviorParameterQueries.allFor(behaviorVersion).map { params =>
      val paramsWithValues = params.zipWithIndex.map { case (ea, i) =>
        val invocationName = AWSLambdaConstants.invocationParamFor(i)
        ParameterWithValue(ea, invocationName, paramValues.get(invocationName))
      }
      BehaviorResponse(event, behaviorVersion, paramsWithValues, activatedTrigger)
    }
  }

  def chooseFor(event: MessageEvent, maybeTeam: Option[Team], maybeLimitToBehavior: Option[Behavior]): DBIO[Option[BehaviorResponse]] = {
    for {
      maybeLimitToBehaviorVersion <- maybeLimitToBehavior.map { limitToBehavior =>
        limitToBehavior.maybeCurrentVersion
      }.getOrElse(DBIO.successful(None))
      triggers <- maybeLimitToBehaviorVersion.map { limitToBehaviorVersion =>
        MessageTriggerQueries.allFor(limitToBehaviorVersion)
      }.getOrElse {
        maybeTeam.map { team =>
          MessageTriggerQueries.allActiveFor(team)
        }.getOrElse(DBIO.successful(Seq()))
      }
      maybeActivatedTrigger <- DBIO.successful(triggers.find(_.isActivatedBy(event)))
      maybeResponse <- maybeActivatedTrigger.map { trigger =>
        for {
          params <- BehaviorParameterQueries.allFor(trigger.behaviorVersion)
          response <- BehaviorResponse.buildFor(event, trigger.behaviorVersion, trigger.invocationParamsFor(event, params), trigger)
        } yield Some(response)
      }.getOrElse(DBIO.successful(None))
    } yield maybeResponse
  }
}

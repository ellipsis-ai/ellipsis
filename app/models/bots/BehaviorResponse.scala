package models.bots

import models.bots.behavior.Behavior
import models.bots.behaviorparameter.BehaviorParameter
import models.bots.behaviorversion.BehaviorVersion
import models.team.Team
import models.bots.conversations.{CollectedParameterValue, InvokeBehaviorConversation}
import models.bots.events.MessageEvent
import models.bots.triggers.messagetrigger.MessageTrigger
import org.joda.time.DateTime
import services.{AWSLambdaConstants, AWSLambdaService, DataService}
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

  def resultForFilledOut(service: AWSLambdaService, dataService: DataService): Future[BehaviorResult] = {
    val startTime = DateTime.now
    dataService.behaviorVersions.resultFor(behaviorVersion, parametersWithValues, event).flatMap { result =>
      val runtimeInMilliseconds = DateTime.now.toDate.getTime - startTime.toDate.getTime
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

  def result(awsService: AWSLambdaService, dataService: DataService): DBIO[BehaviorResult] = {
    if (isFilledOut) {
      DBIO.from(resultForFilledOut(awsService, dataService))
    } else {
      for {
        convo <- InvokeBehaviorConversation.createFor(behaviorVersion, event.context.name, event.context.userIdForContext, activatedTrigger)
        _ <- DBIO.sequence(parametersWithValues.map { p =>
          p.maybeValue.map { v =>
            CollectedParameterValue(p.parameter, convo, v).save
          }.getOrElse(DBIO.successful(Unit))
        })
        result <- convo.resultFor(event, awsService, dataService)
      } yield result
    }
  }
}

object BehaviorResponse {

  def buildFor(
                event: MessageEvent,
                behaviorVersion: BehaviorVersion,
                paramValues: Map[String, String],
                activatedTrigger: MessageTrigger,
                dataService: DataService
                ): Future[BehaviorResponse] = {
    dataService.behaviorParameters.allFor(behaviorVersion).map { params =>
      val paramsWithValues = params.zipWithIndex.map { case (ea, i) =>
        val invocationName = AWSLambdaConstants.invocationParamFor(i)
        ParameterWithValue(ea, invocationName, paramValues.get(invocationName))
      }
      BehaviorResponse(event, behaviorVersion, paramsWithValues, activatedTrigger)
    }
  }

  def chooseFor(event: MessageEvent, maybeTeam: Option[Team], maybeLimitToBehavior: Option[Behavior], dataService: DataService): Future[Option[BehaviorResponse]] = {
    for {
      maybeLimitToBehaviorVersion <- maybeLimitToBehavior.map { limitToBehavior =>
        dataService.behaviors.maybeCurrentVersionFor(limitToBehavior)
      }.getOrElse(Future.successful(None))
      triggers <- maybeLimitToBehaviorVersion.map { limitToBehaviorVersion =>
        dataService.messageTriggers.allFor(limitToBehaviorVersion)
      }.getOrElse {
        maybeTeam.map { team =>
          dataService.messageTriggers.allActiveFor(team)
        }.getOrElse(Future.successful(Seq()))
      }
      maybeActivatedTrigger <- Future.successful(triggers.find(_.isActivatedBy(event)))
      maybeResponse <- maybeActivatedTrigger.map { trigger =>
        for {
          params <- dataService.behaviorParameters.allFor(trigger.behaviorVersion)
          response <- BehaviorResponse.buildFor(event, trigger.behaviorVersion, trigger.invocationParamsFor(event, params), trigger, dataService)
        } yield Some(response)
      }.getOrElse(Future.successful(None))
    } yield maybeResponse
  }
}

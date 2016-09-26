package models.behaviors

import models.behaviors.behavior.Behavior
import models.behaviors.behaviorparameter.BehaviorParameter
import models.behaviors.behaviorversion.BehaviorVersion
import models.team.Team
import models.behaviors.conversations.InvokeBehaviorConversation
import models.behaviors.events.MessageEvent
import models.behaviors.triggers.messagetrigger.MessageTrigger
import org.joda.time.DateTime
import play.api.libs.json.{JsString, JsValue}
import play.api.libs.ws.WSClient
import services.{AWSLambdaConstants, AWSLambdaService, DataService}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class ParameterValue(text: String, json: JsValue, isValid: Boolean)

case class ParameterWithValue(
                               parameter: BehaviorParameter,
                               invocationName: String,
                               maybeValue: Option[ParameterValue]
                             ) {

  val preparedValue: JsValue = maybeValue.map(_.json).getOrElse(JsString(""))

  def hasValidValue: Boolean = maybeValue.exists(_.isValid)
  def hasInvalidValue: Boolean = maybeValue.exists(v => !v.isValid)
}

case class BehaviorResponse(
                             event: MessageEvent,
                             behaviorVersion: BehaviorVersion,
                             parametersWithValues: Seq[ParameterWithValue],
                             activatedTrigger: MessageTrigger,
                             dataService: DataService,
                             lambdaService: AWSLambdaService,
                             ws: WSClient
                             ) {

  def isFilledOut: Boolean = {
    parametersWithValues.forall(_.hasValidValue)
  }

  def resultForFilledOut: Future[BotResult] = {
    val startTime = DateTime.now
    dataService.behaviorVersions.resultFor(behaviorVersion, parametersWithValues, event).flatMap { result =>
      val runtimeInMilliseconds = DateTime.now.toDate.getTime - startTime.toDate.getTime
      dataService.invocationLogEntries.createFor(
        behaviorVersion,
        result,
        event.context.name,
        Some(event.context.userIdForContext),
        runtimeInMilliseconds
      ).map(_ => result)
    }
  }

  def result: Future[BotResult] = {
    if (isFilledOut) {
      resultForFilledOut
    } else {
      for {
        convo <- InvokeBehaviorConversation.createFor(
          behaviorVersion,
          event.context.name,
          event.context.userIdForContext,
          activatedTrigger,
          dataService
        )
        _ <- Future.sequence(parametersWithValues.map { p =>
          p.maybeValue.map { v =>
            dataService.collectedParameterValues.ensureFor(p.parameter, convo, v.text)
          }.getOrElse(Future.successful(Unit))
        })
        result <- convo.resultFor(event, dataService, lambdaService, ws)
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
                dataService: DataService,
                lambdaService: AWSLambdaService,
                ws: WSClient
                ): Future[BehaviorResponse] = {
    for {
      params <- dataService.behaviorParameters.allFor(behaviorVersion)
      invocationNames <- Future.successful(params.zipWithIndex.map { case (p, i) =>
        AWSLambdaConstants.invocationParamFor(i)
      })
      values <- Future.sequence(params.zip(invocationNames).map { case(param, invocationName) =>
        paramValues.get(invocationName).map { v =>
          for {
            isValid <- param.paramType.isValid(v)
            json <- param.paramType.prepareForInvocation(v)
          } yield {
            Some(ParameterValue(v, json, isValid))
          }
        }.getOrElse(Future.successful(None))
      })
    } yield {
      val paramsWithValues = params.zip(values).zip(invocationNames).map { case((param, maybeValue), invocationName) =>
        ParameterWithValue(param, invocationName, maybeValue)
      }
      BehaviorResponse(event, behaviorVersion, paramsWithValues, activatedTrigger, dataService, lambdaService, ws)
    }
  }

  def chooseFor(
                 event: MessageEvent,
                 maybeTeam: Option[Team],
                 maybeLimitToBehavior: Option[Behavior],
                 dataService: DataService,
                 lambdaService: AWSLambdaService,
                 ws: WSClient
               ): Future[Option[BehaviorResponse]] = {
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
          response <-
            BehaviorResponse.buildFor(
              event,
              trigger.behaviorVersion,
              trigger.invocationParamsFor(event, params),
              trigger,
              dataService,
              lambdaService,
              ws
            )
        } yield Some(response)
      }.getOrElse(Future.successful(None))
    } yield maybeResponse
  }
}

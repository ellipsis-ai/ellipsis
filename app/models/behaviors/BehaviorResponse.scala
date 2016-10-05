package models.behaviors

import models.behaviors.behavior.Behavior
import models.behaviors.behaviorparameter.{BehaviorParameter, BehaviorParameterContext}
import models.behaviors.behaviorversion.BehaviorVersion
import models.team.Team
import models.behaviors.conversations.InvokeBehaviorConversation
import models.behaviors.conversations.conversation.Conversation
import models.behaviors.events.MessageEvent
import models.behaviors.triggers.messagetrigger.MessageTrigger
import org.joda.time.DateTime
import play.api.cache.CacheApi
import play.api.libs.json.{JsString, JsValue}
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
                             lambdaService: AWSLambdaService,
                             dataService: DataService,
                             cache: CacheApi
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
        result <- convo.resultFor(event, lambdaService, dataService, cache)
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
                maybeConversation: Option[Conversation],
                lambdaService: AWSLambdaService,
                dataService: DataService,
                cache: CacheApi
                ): Future[BehaviorResponse] = {
    for {
      params <- dataService.behaviorParameters.allFor(behaviorVersion)
      invocationNames <- Future.successful(params.zipWithIndex.map { case (p, i) =>
        AWSLambdaConstants.invocationParamFor(i)
      })
      values <- Future.sequence(params.zip(invocationNames).map { case(param, invocationName) =>
        val context = BehaviorParameterContext(event, maybeConversation, param, cache, dataService)
        paramValues.get(invocationName).map { v =>
          for {
            isValid <- param.paramType.isValid(v, context)
            json <- param.paramType.prepareForInvocation(v, context)
          } yield {
            Some(ParameterValue(v, json, isValid))
          }
        }.getOrElse(Future.successful(None))
      })
    } yield {
      val paramsWithValues = params.zip(values).zip(invocationNames).map { case((param, maybeValue), invocationName) =>
        ParameterWithValue(param, invocationName, maybeValue)
      }
      BehaviorResponse(event, behaviorVersion, paramsWithValues, activatedTrigger, lambdaService, dataService, cache)
    }
  }

  def allFor(
                 event: MessageEvent,
                 maybeTeam: Option[Team],
                 maybeLimitToBehavior: Option[Behavior],
                 lambdaService: AWSLambdaService,
                 dataService: DataService,
                 cache: CacheApi
               ): Future[Seq[BehaviorResponse]] = {
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
      activatedTriggerLists <- Future.successful {
        triggers.
          filter(_.isActivatedBy(event)).
          groupBy(_.behaviorVersion).
          values.
          toSeq
      }
      activatedTriggerListsWithParamCounts <- Future.sequence(
        activatedTriggerLists.map { list =>
          Future.sequence(list.map { trigger =>
            for {
              params <- dataService.behaviorParameters.allFor(trigger.behaviorVersion)
            } yield {
              (trigger, trigger.invocationParamsFor(event, params).size)
            }
          })
        }
      )
      // we want to chose activated triggers with more params first
      activatedTriggers <- Future.successful(activatedTriggerListsWithParamCounts.flatMap { list =>
        list.
          sortBy { case(_, paramCount) => paramCount }.
          map { case(trigger, _) => trigger }.
          reverse.
          headOption
      })
      responses <- Future.sequence(activatedTriggers.map { trigger =>
        for {
          params <- dataService.behaviorParameters.allFor(trigger.behaviorVersion)
          response <-
            BehaviorResponse.buildFor(
              event,
              trigger.behaviorVersion,
              trigger.invocationParamsFor(event, params),
              trigger,
              None,
              lambdaService,
              dataService,
              cache
            )
        } yield response
      })
    } yield responses
  }
}

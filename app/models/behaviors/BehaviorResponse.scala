package models.behaviors

import java.time.OffsetDateTime

import models.behaviors.behavior.Behavior
import models.behaviors.behaviorparameter.{BehaviorParameter, BehaviorParameterContext}
import models.behaviors.behaviorversion.BehaviorVersion
import models.team.Team
import models.behaviors.conversations.InvokeBehaviorConversation
import models.behaviors.conversations.conversation.Conversation
import models.behaviors.events.MessageEvent
import models.behaviors.triggers.messagetrigger.MessageTrigger
import play.api.Configuration
import play.api.cache.CacheApi
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
                             lambdaService: AWSLambdaService,
                             dataService: DataService,
                             cache: CacheApi,
                             ws: WSClient,
                             configuration: Configuration
                             ) {

  def hasAllParamValues: Boolean = {
    parametersWithValues.forall(_.hasValidValue)
  }

  def hasAllUserEnvVarValues: Future[Boolean] = {
    for {
      user <- event.ensureUser(dataService)
      missing <- dataService.userEnvironmentVariables.missingFor(user, behaviorVersion, dataService)
    } yield missing.isEmpty
  }

  def hasAllSimpleTokens: Future[Boolean] = {
    for {
      user <- event.ensureUser(dataService)
      missing <- dataService.requiredSimpleTokenApis.missingFor(user, behaviorVersion)
    } yield missing.isEmpty
  }

  def isReady: Future[Boolean] = {
    for {
      hasSimpleTokens <- hasAllSimpleTokens
      hasUserEnvVars <- hasAllUserEnvVarValues
    } yield {
      hasSimpleTokens && hasUserEnvVars && hasAllParamValues
    }
  }

  def resultForFilledOut: Future[BotResult] = {
    val startTime = OffsetDateTime.now
    for {
      user <- event.ensureUser(dataService)
      result <- dataService.behaviorVersions.resultFor(behaviorVersion, parametersWithValues, event)
      _ <- {
        val runtimeInMilliseconds = OffsetDateTime.now.toInstant.toEpochMilli - startTime.toInstant.toEpochMilli
        dataService.invocationLogEntries.createFor(
          behaviorVersion,
          parametersWithValues,
          result,
          event,
          Some(event.userIdForContext),
          user,
          runtimeInMilliseconds
        )
      }
    } yield result
  }

  def result: Future[BotResult] = {
    dataService.behaviorVersions.maybeNotReadyResultFor(behaviorVersion, event).flatMap { maybeNotReadyResult =>
      maybeNotReadyResult.map(Future.successful).getOrElse {
        isReady.flatMap { ready =>
          if (ready) {
            resultForFilledOut
          } else {
            for {
              context <- event.conversationContextFor(behaviorVersion)
              convo <- InvokeBehaviorConversation.createFor(
                behaviorVersion,
                context,
                event.userIdForContext,
                activatedTrigger,
                dataService
              )
              _ <- Future.sequence(parametersWithValues.map { p =>
                p.maybeValue.map { v =>
                  dataService.collectedParameterValues.ensureFor(p.parameter, convo, v.text)
                }.getOrElse(Future.successful(Unit))
              })
              result <- convo.resultFor(event, lambdaService, dataService, cache, ws, configuration)
            } yield result
          }
        }
      }
    }
  }
}

object BehaviorResponse {

  def parametersWithValuesFor(
                               event: MessageEvent,
                               behaviorVersion: BehaviorVersion,
                               paramValues: Map[String, String],
                               maybeConversation: Option[Conversation],
                               dataService: DataService,
                               cache: CacheApi,
                               configuration: Configuration
                             ): Future[Seq[ParameterWithValue]] = {
    for {
      params <- dataService.behaviorParameters.allFor(behaviorVersion)
      invocationNames <- Future.successful(params.zipWithIndex.map { case (p, i) =>
        AWSLambdaConstants.invocationParamFor(i)
      })
      values <- Future.sequence(params.zip(invocationNames).map { case(param, invocationName) =>
        val context = BehaviorParameterContext(event, maybeConversation, param, cache, dataService, configuration)
        paramValues.get(invocationName).map { v =>
          for {
            isValid <- param.paramType.isValid(v, context)
            json <- param.paramType.prepareForInvocation(v, context)
          } yield {
            Some(ParameterValue(v, json, isValid))
          }
        }.getOrElse(Future.successful(None))
      })
    } yield params.zip(values).zip(invocationNames).map { case((param, maybeValue), invocationName) =>
        ParameterWithValue(param, invocationName, maybeValue)
      }
  }

  def buildFor(
                event: MessageEvent,
                behaviorVersion: BehaviorVersion,
                paramValues: Map[String, String],
                activatedTrigger: MessageTrigger,
                maybeConversation: Option[Conversation],
                lambdaService: AWSLambdaService,
                dataService: DataService,
                cache: CacheApi,
                ws: WSClient,
                configuration: Configuration
                ): Future[BehaviorResponse] = {
    parametersWithValuesFor(event, behaviorVersion, paramValues, maybeConversation, dataService, cache, configuration).map { paramsWithValues =>
      BehaviorResponse(event, behaviorVersion, paramsWithValues, activatedTrigger, lambdaService, dataService, cache, ws, configuration)
    }
  }

  def allFor(
              event: MessageEvent,
              maybeTeam: Option[Team],
              maybeLimitToBehavior: Option[Behavior],
              lambdaService: AWSLambdaService,
              dataService: DataService,
              cache: CacheApi,
              ws: WSClient,
              configuration: Configuration
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
              cache,
              ws,
              configuration
            )
        } yield response
      })
    } yield responses
  }
}

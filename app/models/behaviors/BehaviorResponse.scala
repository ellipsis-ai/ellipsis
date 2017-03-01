package models.behaviors

import java.time.OffsetDateTime

import akka.actor.ActorSystem
import models.behaviors.behavior.Behavior
import models.behaviors.behaviorparameter.{BehaviorParameter, BehaviorParameterContext}
import models.behaviors.behaviorversion.BehaviorVersion
import models.team.Team
import models.behaviors.conversations.InvokeBehaviorConversation
import models.behaviors.conversations.conversation.Conversation
import models.behaviors.events.Event
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
                             event: Event,
                             behaviorVersion: BehaviorVersion,
                             parametersWithValues: Seq[ParameterWithValue],
                             maybeActivatedTrigger: Option[MessageTrigger],
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

  def result(implicit actorSystem: ActorSystem): Future[BotResult] = {
    dataService.behaviorVersions.maybeNotReadyResultFor(behaviorVersion, event).flatMap { maybeNotReadyResult =>
      maybeNotReadyResult.map(Future.successful).getOrElse {
        isReady.flatMap { ready =>
          if (ready) {
            resultForFilledOut
          } else {
            for {
              maybeChannel <- event.maybeChannelToUseFor(behaviorVersion, dataService)
              convo <- InvokeBehaviorConversation.createFor(
                behaviorVersion,
                event,
                maybeChannel,
                maybeActivatedTrigger,
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
                               event: Event,
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
                event: Event,
                behaviorVersion: BehaviorVersion,
                paramValues: Map[String, String],
                maybeActivatedTrigger: Option[MessageTrigger],
                maybeConversation: Option[Conversation],
                lambdaService: AWSLambdaService,
                dataService: DataService,
                cache: CacheApi,
                ws: WSClient,
                configuration: Configuration
                ): Future[BehaviorResponse] = {
    parametersWithValuesFor(event, behaviorVersion, paramValues, maybeConversation, dataService, cache, configuration).map { paramsWithValues =>
      BehaviorResponse(event, behaviorVersion, paramsWithValues, maybeActivatedTrigger, lambdaService, dataService, cache, ws, configuration)
    }
  }

  def allFor(
              event: Event,
              maybeTeam: Option[Team],
              maybeLimitToBehavior: Option[Behavior],
              lambdaService: AWSLambdaService,
              dataService: DataService,
              cache: CacheApi,
              ws: WSClient,
              configuration: Configuration
               ): Future[Seq[BehaviorResponse]] = {
    event.allBehaviorResponsesFor(maybeTeam, maybeLimitToBehavior, lambdaService, dataService, cache, ws, configuration)
  }
}

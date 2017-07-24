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
import slick.dbio.DBIO

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
                             maybeConversation: Option[Conversation],
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
      missing <- dataService.requiredSimpleTokenApis.missingFor(user, behaviorVersion.groupVersion)
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

  def resultForFilledOutAction: DBIO[BotResult] = {
    val startTime = OffsetDateTime.now
    for {
      user <- event.ensureUserAction(dataService)
      result <- dataService.behaviorVersions.resultForAction(behaviorVersion, parametersWithValues, event, maybeConversation)
      _ <- {
        val runtimeInMilliseconds = OffsetDateTime.now.toInstant.toEpochMilli - startTime.toInstant.toEpochMilli
        dataService.invocationLogEntries.createForAction(
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

  def resultForFilledOut: Future[BotResult] = {
    dataService.run(resultForFilledOutAction)
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
              result <- convo.resultFor(event, lambdaService, dataService, cache, ws, configuration, actorSystem)
            } yield result
          }
        }
      }
    }
  }
}

object BehaviorResponse {

  def parametersWithValuesForAction(
                                     event: Event,
                                     behaviorVersion: BehaviorVersion,
                                     paramValues: Map[String, String],
                                     maybeConversation: Option[Conversation],
                                     dataService: DataService,
                                     cache: CacheApi,
                                     configuration: Configuration,
                                     actorSystem: ActorSystem
                                   ): DBIO[Seq[ParameterWithValue]] = {
    for {
      params <- dataService.behaviorParameters.allForAction(behaviorVersion)
      invocationNames <- DBIO.successful(params.zipWithIndex.map { case (p, i) =>
        AWSLambdaConstants.invocationParamFor(i)
      })
      values <- DBIO.sequence(params.zip(invocationNames).map { case(param, invocationName) =>
        val context = BehaviorParameterContext(event, maybeConversation, param, cache, dataService, configuration, actorSystem)
        paramValues.get(invocationName).map { v =>
          for {
            isValid <- DBIO.from(param.paramType.isValid(v, context))
            json <- DBIO.from(param.paramType.prepareForInvocation(v, context))
          } yield {
            Some(ParameterValue(v, json, isValid))
          }
        }.getOrElse(DBIO.successful(None))
      })
    } yield params.zip(values).zip(invocationNames).map { case((param, maybeValue), invocationName) =>
      ParameterWithValue(param, invocationName, maybeValue)
    }
  }

  def parametersWithValuesFor(
                               event: Event,
                               behaviorVersion: BehaviorVersion,
                               paramValues: Map[String, String],
                               maybeConversation: Option[Conversation],
                               dataService: DataService,
                               cache: CacheApi,
                               configuration: Configuration,
                               actorSystem: ActorSystem
                             ): Future[Seq[ParameterWithValue]] = {
    dataService.run(parametersWithValuesForAction(event, behaviorVersion, paramValues, maybeConversation, dataService, cache, configuration, actorSystem))
  }

  def buildForAction(
                      event: Event,
                      behaviorVersion: BehaviorVersion,
                      paramValues: Map[String, String],
                      maybeActivatedTrigger: Option[MessageTrigger],
                      maybeConversation: Option[Conversation],
                      lambdaService: AWSLambdaService,
                      dataService: DataService,
                      cache: CacheApi,
                      ws: WSClient,
                      configuration: Configuration,
                      actorSystem: ActorSystem
                    ): DBIO[BehaviorResponse] = {
    parametersWithValuesForAction(event, behaviorVersion, paramValues, maybeConversation, dataService, cache, configuration, actorSystem).map { paramsWithValues =>
      BehaviorResponse(event, behaviorVersion, maybeConversation, paramsWithValues, maybeActivatedTrigger, lambdaService, dataService, cache, ws, configuration)
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
                configuration: Configuration,
                actorSystem: ActorSystem
                ): Future[BehaviorResponse] = {
    dataService.run(buildForAction(event, behaviorVersion, paramValues, maybeActivatedTrigger, maybeConversation, lambdaService, dataService, cache, ws, configuration, actorSystem))
  }

  def allFor(
              event: Event,
              maybeTeam: Option[Team],
              maybeLimitToBehavior: Option[Behavior],
              lambdaService: AWSLambdaService,
              dataService: DataService,
              cache: CacheApi,
              ws: WSClient,
              configuration: Configuration,
              actorSystem: ActorSystem
               ): Future[Seq[BehaviorResponse]] = {
    event.allBehaviorResponsesFor(maybeTeam, maybeLimitToBehavior, lambdaService, dataService, cache, ws, configuration, actorSystem)
  }
}

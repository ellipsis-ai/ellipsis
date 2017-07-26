package models.behaviors

import java.time.OffsetDateTime

import akka.actor.ActorSystem
import models.behaviors.behaviorparameter.BehaviorParameter
import models.behaviors.behaviorversion.BehaviorVersion
import models.behaviors.conversations.conversation.Conversation
import models.behaviors.conversations.{ConversationServices, InvokeBehaviorConversation}
import models.behaviors.events.Event
import models.behaviors.triggers.messagetrigger.MessageTrigger
import play.api.Configuration
import play.api.libs.json.{JsString, JsValue}
import play.api.libs.ws.WSClient
import services.{AWSLambdaService, CacheService, DataService, SlackEventService}
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
                             slackEventService: SlackEventService,
                             cacheService: CacheService,
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
              maybeChannel <- event.maybeChannelToUseFor(behaviorVersion)
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
              services <- Future.successful(ConversationServices(dataService, lambdaService, slackEventService, cache, configuration, ws, actorSystem))
              result <- convo.resultFor(event, services)
            } yield result
          }
        }
      }
    }
  }
}

package models.behaviors

import java.time.OffsetDateTime

import akka.actor.ActorSystem
import models.accounts.linkedaccount.LinkedAccount
import models.behaviors.behaviorparameter.{BehaviorParameter, BehaviorParameterContext}
import models.behaviors.behaviorversion.{BehaviorVersion, Threaded}
import models.behaviors.conversations.InvokeBehaviorConversation
import models.behaviors.conversations.conversation.Conversation
import models.behaviors.conversations.parentconversation.NewParentConversation
import models.behaviors.events.Event
import models.behaviors.triggers.Trigger
import play.api.Logger
import play.api.libs.json.{JsString, JsValue}
import services._
import services.caching.CacheService
import services.slack.SlackApiError
import slick.dbio.DBIO

import scala.concurrent.{ExecutionContext, Future}

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
                             maybeActivatedTrigger: Option[Trigger],
                             maybeNewParent: Option[NewParentConversation],
                             userExpectsResponse: Boolean,
                             services: DefaultServices
                             ) {

  val dataService: DataService = services.dataService
  val cacheService: CacheService = services.cacheService

  def hasAllParamValues: Boolean = {
    parametersWithValues.forall(_.hasValidValue)
  }

  def hasAllSimpleTokens(implicit ec: ExecutionContext): Future[Boolean] = {
    for {
      user <- event.ensureUser(dataService)
      missing <- dataService.requiredSimpleTokenApis.missingFor(user, behaviorVersion.groupVersion)
    } yield missing.isEmpty
  }

  def isReady(implicit ec: ExecutionContext): Future[Boolean] = {
    for {
      hasSimpleTokens <- hasAllSimpleTokens
    } yield {
      hasSimpleTokens && hasAllParamValues
    }
  }

  def notifyAdmins(result: BotResult)(implicit actorSystem: ActorSystem, ec: ExecutionContext): Future[Unit] = {
    val msg = result.fullText
    for {
      maybeAdminTeamEvent <- services.dataService.slackBotProfiles.eventualMaybeEvent(
        LinkedAccount.ELLIPSIS_SLACK_TEAM_ID,
        LinkedAccount.ELLIPSIS_MANAGED_SKILL_ERRORS_CHANNEL_ID,
        None,
        Some(event.originalEventType)
      )
      wasSent <- maybeAdminTeamEvent.map { adminTeamEvent =>
        val resultToSend = AdminSkillErrorNotificationResult(adminTeamEvent, result)
        services.botResultService.sendIn(resultToSend, None).map(_.isDefined).recover {
          case e: SlackApiError => {
            false
          }
        }
      }.getOrElse(Future.successful(false))
    } yield {
      if (wasSent) {
        Logger.info(s"Managed skill error: $msg")
      } else {
        Logger.error(s"Managed skill error failed to send: $msg")
      }
      {}
    }
  }

  def notifyAdminsIfNec(result: BotResult)(implicit actorSystem: ActorSystem, ec: ExecutionContext): Future[Unit] = {
    for {
      shouldNotify <- result.shouldNotifyAdmins
      _ <- if (shouldNotify) {
        notifyAdmins(result)
      } else {
        Future.successful({})
      }
    } yield {}
  }

  def resultForFilledOutAction(implicit actorSystem: ActorSystem, ec: ExecutionContext): DBIO[BotResult] = {
    val startTime = OffsetDateTime.now
    for {
      user <- event.ensureUserAction(dataService)
      initialResult <- dataService.behaviorVersions.resultForAction(behaviorVersion, parametersWithValues, event, maybeConversation)
      result <- {
        services.dataService.parentConversations.maybeForAction(maybeConversation).flatMap { maybeParent =>
          maybeParent.map { p =>
            val context = BehaviorParameterContext(event, Some(p.parent), p.param, services)
            p.param.paramType.promptResultWithValidValuesResult(initialResult, context)
          }.getOrElse(DBIO.successful(initialResult))
        }
      }
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
      _ <- DBIO.from(notifyAdminsIfNec(result))
    } yield result
  }

  def resultForFilledOut(implicit actorSystem: ActorSystem, ec: ExecutionContext): Future[BotResult] = {
    dataService.run(resultForFilledOutAction)
  }

  def result(implicit actorSystem: ActorSystem, ec: ExecutionContext): Future[BotResult] = {
    dataService.behaviorVersions.maybeNotReadyResultFor(behaviorVersion, event).flatMap { maybeNotReadyResult =>
      maybeNotReadyResult.map(Future.successful).getOrElse {
        isReady.flatMap { ready =>
          if (ready) {
            resultForFilledOut
          } else {
            for {
              maybeChannel <- event.maybeChannelToUseFor(behaviorVersion, services)
              maybeThreadId <- event.maybeThreadId.map(tid => Future.successful(Some(tid))).getOrElse {
                if (behaviorVersion.responseType == Threaded) {
                  event.sendMessage(
                    "Let's continue this in a thread :speech_balloon:",
                    behaviorVersion.responseType,
                    maybeShouldUnfurl = None,
                    None,
                    attachmentGroups = Seq(),
                    files = Seq(),
                    choices = Seq(),
                    DeveloperContext.default,
                    services
                  )
                } else {
                  Future.successful(None)
                }
              }
              convo <- InvokeBehaviorConversation.createFor(
                behaviorVersion,
                event,
                maybeChannel,
                maybeThreadId,
                maybeActivatedTrigger,
                maybeNewParent,
                dataService,
                cacheService
              )
              _ <- Future.sequence(parametersWithValues.map { p =>
                p.maybeValue.map { v =>
                  dataService.collectedParameterValues.ensureFor(p.parameter, convo, v.text)
                }.getOrElse(Future.successful(Unit))
              })
              result <- dataService.run(convo.resultForAction(event, services))
            } yield result
          }
        }
      }
    }
  }
}

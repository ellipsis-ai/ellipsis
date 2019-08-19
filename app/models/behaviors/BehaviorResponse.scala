package models.behaviors

import java.time.OffsetDateTime

import akka.actor.ActorSystem
import models.accounts.linkedaccount.LinkedAccount
import models.behaviors.behaviorparameter.{BehaviorParameter, BehaviorParameterContext}
import models.behaviors.behaviorversion._
import models.behaviors.conversations.InvokeBehaviorConversation
import models.behaviors.conversations.conversation.Conversation
import models.behaviors.conversations.parentconversation.NewParentConversation
import models.behaviors.dialogs.Dialog
import models.behaviors.events.Event
import models.behaviors.messagelistener.MessageListener
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
                             maybeDialog: Option[DialogInfo],
                             userExpectsResponse: Boolean,
                             maybeMessageListener: Option[MessageListener],
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
      maybeAdminTeamEvent <- services.dataService.slackBotProfiles.eventualMaybeManagedSkillErrorEvent(event.originalEventType)
      wasSent <- maybeAdminTeamEvent.map { adminTeamEvent =>
        val resultToSend = AdminSkillErrorNotificationResult(services.configuration, adminTeamEvent, result, None)
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
      initialResult <- dataService.behaviorVersions.resultForAction(behaviorVersion, parametersWithValues, event, maybeConversation, maybeMessageListener.exists(_.isForCopilot))
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
          Some(event.eventContext.userIdForContext),
          user,
          runtimeInMilliseconds,
          maybeMessageListener
        )
      }
      _ <- DBIO.from(notifyAdminsIfNec(result))
    } yield result
  }

  def resultForFilledOut(implicit actorSystem: ActorSystem, ec: ExecutionContext): Future[BotResult] = {
    dataService.run(resultForFilledOutAction)
  }

  private def maybeThreadIdToUse(implicit actorSystem: ActorSystem, ec: ExecutionContext): Future[Option[String]] = {
    event.maybeThreadId.map { tid =>
      if (behaviorVersion.responseType == Private && !event.eventContext.isDirectMessage) {
        Future.successful(None)
      } else {
        Future.successful(Some(tid))
      }
    }.getOrElse {
      if (behaviorVersion.responseType == Threaded) {
        maybeStartThreadRoot
      } else {
        Future.successful(None)
      }
    }
  }

  private def maybeStartThreadRoot(implicit actorSystem: ActorSystem, ec: ExecutionContext): Future[Option[String]] = {
    if (event.isEphemeral) {
      Future.successful(None)
    } else {
      event.sendMessage(
        "Let’s continue this in a thread. :speech_balloon:",
        Some(behaviorVersion),
        Normal,
        maybeShouldUnfurl = None,
        None,
        attachments = Seq(),
        files = Seq(),
        choices = Seq(),
        DeveloperContext.default,
        services
      ).map(_.flatMap(_.maybeId))
    }
  }

  def result(implicit actorSystem: ActorSystem, ec: ExecutionContext): Future[BotResult] = {
    for {
      maybeNotReadyResult <- dataService.behaviorVersions.maybeNotReadyResultFor(behaviorVersion, event)
      result <- maybeNotReadyResult.map(Future.successful).getOrElse {
        for {
          ready <- isReady
          readyResult <- if (ready) {
            resultForFilledOut
          } else {
            for {
              maybeDialogResult <- maybeDialog.map { dialogInfo =>
                val context = DeveloperContext.default
                Dialog(
                  behaviorVersion.maybeName,
                  behaviorVersion,
                  event,
                  dialogInfo,
                  parametersWithValues,
                  context,
                  services
                ).maybeResult
              }.getOrElse(Future.successful(None))
              notFilledOutResult <- maybeDialogResult.map(Future.successful).getOrElse {
                for {
                  maybeChannel <- event.maybeChannelToUseFor(behaviorVersion, services)
                  maybeThreadId <- maybeThreadIdToUse
                  convo <- InvokeBehaviorConversation.createFor(
                    behaviorVersion,
                    event,
                    maybeChannel,
                    maybeThreadId,
                    maybeActivatedTrigger,
                    maybeNewParent,
                    services
                  )
                  _ <- Future.sequence(parametersWithValues.map { p =>
                    p.maybeValue.map { v =>
                      dataService.collectedParameterValues.ensureFor(p.parameter, convo, v.text)
                    }.getOrElse(Future.successful(Unit))
                  })
                  convoResult <- dataService.run(convo.resultForAction(event, services))
                } yield convoResult
              }
            } yield notFilledOutResult
          }
        } yield readyResult
      }
    } yield result
  }
}

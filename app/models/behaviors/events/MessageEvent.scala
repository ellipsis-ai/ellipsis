package models.behaviors.events

import models.behaviors.BehaviorResponse
import models.behaviors.behavior.Behavior
import models.behaviors.conversations.conversation.Conversation
import models.team.Team
import play.api.Configuration
import play.api.cache.CacheApi
import play.api.libs.ws.WSClient
import services.{AWSLambdaService, DataService}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.matching.Regex

trait MessageEvent extends Event {

  lazy val invocationLogText: String = relevantMessageText

  def isDirectMessage(channel: String): Boolean

  def allOngoingConversations(dataService: DataService): Future[Seq[Conversation]] = {
    dataService.conversations.allOngoingFor(userIdForContext, context, maybeChannel, maybeThreadId, maybeChannel.exists(isDirectMessage))
  }

  def allBehaviorResponsesFor(
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
          filter(_.isActivatedBy(this)).
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
              (trigger, trigger.invocationParamsFor(this, params).size)
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
            this,
            trigger.behaviorVersion,
            trigger.invocationParamsFor(this, params),
            Some(trigger),
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

object MessageEvent {

  def ellipsisRegex: Regex = """^(\.\.\.|…)""".r
}

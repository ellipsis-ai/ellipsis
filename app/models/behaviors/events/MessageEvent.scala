package models.behaviors.events

import models.behaviors.BehaviorResponse
import models.behaviors.behavior.Behavior
import models.behaviors.conversations.conversation.Conversation
import models.team.Team
import services.{DataService, DefaultServices}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.matching.Regex

trait MessageEvent extends Event {

  lazy val invocationLogText: String = relevantMessageText

  def allOngoingConversations(dataService: DataService): Future[Seq[Conversation]] = {
    dataService.conversations.allOngoingFor(userIdForContext, context, maybeChannel, maybeThreadId, teamId)
  }

  def allBehaviorResponsesFor(
                              maybeTeam: Option[Team],
                              maybeLimitToBehavior: Option[Behavior],
                              services: DefaultServices
                            )(implicit ec: ExecutionContext): Future[Seq[BehaviorResponse]] = {
    val dataService = services.dataService
    for {
      maybeLimitToBehaviorVersion <- maybeLimitToBehavior.map { limitToBehavior =>
        dataService.behaviors.maybeCurrentVersionFor(limitToBehavior)
      }.getOrElse(Future.successful(None))
      triggers <- maybeLimitToBehaviorVersion.map { limitToBehaviorVersion =>
        dataService.messageTriggers.allFor(limitToBehaviorVersion)
      }.getOrElse {
        (for {
          team <- maybeTeam
          channel <- maybeChannel
        } yield {
          dataService.behaviorGroupDeployments.allActiveTriggersFor(context, channel, team)
        }).getOrElse(Future.successful(Seq()))
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
          response <- dataService.behaviorResponses.buildFor(
            this,
            trigger.behaviorVersion,
            trigger.invocationParamsFor(this, params),
            Some(trigger),
            None
          )
        } yield response
      })
    } yield responses
  }

}

object MessageEvent {

  def ellipsisRegex: Regex = """^(\.\.\.|…)\s*""".r
}

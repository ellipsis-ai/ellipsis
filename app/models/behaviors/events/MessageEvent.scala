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
      listeners <- dataService.messageListeners.allFor(this, maybeTeam, maybeChannel, context)
      listenerResponses <- Future.sequence(listeners.map { ea =>
        for {
          maybeBehaviorVersion <- dataService.behaviors.maybeCurrentVersionFor(ea.behavior)
          maybeResponse <- maybeBehaviorVersion.map { behaviorVersion =>
            for {
              params <- dataService.behaviorParameters.allFor(behaviorVersion)
              maybeMessageInput <- dataService.inputs.findByInputId(ea.messageInputId, behaviorVersion.groupVersion)
              maybeResponse <- maybeMessageInput.map { messageInput =>
                dataService.behaviorResponses.buildFor(
                  this,
                  behaviorVersion,
                  ea.invocationParamsFor(params, messageInput, relevantMessageText),
                  None,
                  None,
                  None
                ).map(Some(_))
              }.getOrElse(Future.successful(None))
            } yield maybeResponse
          }.getOrElse(Future.successful(None))
        } yield maybeResponse
      }).map(_.flatten)
      possibleActivatedTriggers <- dataService.behaviorGroupDeployments.possibleActivatedTriggersFor(this, maybeTeam, maybeChannel, context, maybeLimitToBehavior)
      activatedTriggers <- activatedTriggersIn(possibleActivatedTriggers, dataService)
      triggerResponses <- Future.sequence(activatedTriggers.map { trigger =>
        for {
          params <- dataService.behaviorParameters.allFor(trigger.behaviorVersion)
          response <- dataService.behaviorResponses.buildFor(
            this,
            trigger.behaviorVersion,
            trigger.invocationParamsFor(this, params),
            Some(trigger),
            None,
            None
          )
        } yield response
      })
    } yield triggerResponses ++ listenerResponses
  }

}

object MessageEvent {

  def ellipsisShortcutMentionRegex: Regex = """^(\.\.\.|…)\s*""".r
}

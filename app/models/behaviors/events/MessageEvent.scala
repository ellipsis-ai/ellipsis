package models.behaviors.events

import akka.actor.ActorSystem
import models.behaviors.BehaviorResponse
import models.behaviors.behavior.Behavior
import models.behaviors.conversations.conversation.Conversation
import models.behaviors.ellipsisobject.Message
import models.team.Team
import services.DefaultServices
import slick.dbio.DBIO
import utils.FileReference

import scala.concurrent.{ExecutionContext, Future}
import scala.util.matching.Regex

trait MessageEvent extends Event {

  lazy val invocationLogText: String = relevantMessageText

  val maybeFile: Option[FileReference]
  override def hasFile: Boolean = maybeFile.isDefined

  def maybeNewFileId(services: DefaultServices): Option[String] = maybeFile.map { file =>
    services.fileMap.save(file)
  }

  override def maybeMessageInfoAction(
                              maybeConversation: Option[Conversation],
                              services: DefaultServices
                            )(implicit actorSystem: ActorSystem, ec: ExecutionContext): DBIO[Option[Message]] = {
    Message.buildForAction(this, maybeConversation, services).map(Some(_))
  }

  def allBehaviorResponsesFor(
                              maybeTeam: Option[Team],
                              maybeLimitToBehavior: Option[Behavior],
                              services: DefaultServices
                            )(implicit ec: ExecutionContext): Future[Seq[BehaviorResponse]] = {
    val dataService = services.dataService
    for {
      listeners <- dataService.messageListeners.allFor(this, maybeTeam, maybeChannel, eventContext.name)
      listenerResponses <- Future.sequence(listeners.map { ea =>
        for {
          maybeGroupVersion <- dataService.behaviorGroupDeployments.maybeActiveBehaviorGroupVersionFor(ea.behavior.group, ea.medium, ea.channel)
          maybeBehaviorVersion <- maybeGroupVersion.map { groupVersion =>
            dataService.behaviorVersions.findFor(ea.behavior, groupVersion)
          }.getOrElse(Future.successful(None))
          maybeResponse <- maybeBehaviorVersion.map { behaviorVersion =>
            for {
              params <- dataService.behaviorParameters.allFor(behaviorVersion)
              maybeResponse <- dataService.behaviorResponses.buildFor(
                this,
                behaviorVersion,
                ea.invocationParamsFor(params, relevantMessageText),
                None,
                None,
                None,
                userExpectsResponse = false
              ).map(Some(_))
            } yield maybeResponse
          }.getOrElse(Future.successful(None))
        } yield maybeResponse
      }).map(_.flatten)
      possibleActivatedTriggers <- dataService.behaviorGroupDeployments.possibleActivatedTriggersFor(this, maybeTeam, maybeChannel, eventContext.name, maybeLimitToBehavior)
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
            None,
            userExpectsResponse = true
          )
        } yield response
      })
    } yield triggerResponses ++ listenerResponses
  }

}

object MessageEvent {

  def ellipsisShortcutMentionRegex: Regex = """^(\.\.\.|…)\s*""".r
}

package models.behaviors.dialogs

import akka.actor.ActorSystem
import json.DialogState
import models.behaviors.behaviorversion.BehaviorVersion
import models.behaviors.events.Event
import models.behaviors._
import services.DefaultServices

import scala.concurrent.{ExecutionContext, Future}

case class Dialog(
                   maybeTitle: Option[String],
                   behaviorVersion: BehaviorVersion,
                   event: Event,
                   dialogInfo: DialogInfo,
                   parametersWithValues: Seq[ParameterWithValue],
                   developerContext: DeveloperContext,
                   services: DefaultServices
                 )(implicit actorSystem: ActorSystem, ec: ExecutionContext) {

  def state: DialogState = {
    event.eventContext.stateForDialog(event, parametersWithValues)
  }

  def maybeResult: Future[Option[BotResult]] = {
    event.eventContext.maybeOpenDialog(event, this, developerContext, services).map { maybeDidOpen =>
      if (maybeDidOpen.contains(true)) {
        Some(DialogResult(event, this, behaviorVersion, parametersWithValues, developerContext))
      } else {
        None
      }
    }
  }
}


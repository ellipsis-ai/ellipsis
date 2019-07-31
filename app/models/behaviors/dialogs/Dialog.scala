package models.behaviors.dialogs

import akka.actor.ActorSystem
import models.behaviors.{BotResult, DeveloperContext, DialogInfo, DialogResult, ParameterWithValue}
import models.behaviors.behaviorversion.BehaviorVersion
import models.behaviors.events.Event
import services.DefaultServices

import scala.concurrent.{ExecutionContext, Future}

case class Dialog(
                   behaviorVersion: BehaviorVersion,
                   event: Event,
                   dialogInfo: DialogInfo,
                   parametersWithValues: Seq[ParameterWithValue],
                   developerContext: DeveloperContext,
                   services: DefaultServices
                 )(implicit actorSystem: ActorSystem, ec: ExecutionContext) {
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


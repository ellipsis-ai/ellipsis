package models.behaviors.builtins

import akka.actor.ActorSystem
import models.behaviors.events.Event
import models.behaviors.{BotResult, SimpleTextResult}
import play.api.Configuration
import services.{AWSLambdaService, DataService}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class UnscheduleBehavior(
                               text: String,
                               event: Event,
                               lambdaService: AWSLambdaService,
                               dataService: DataService,
                               configuration: Configuration
                             ) extends BuiltinBehavior {

  def result(implicit actorSystem: ActorSystem): Future[BotResult] = {
    for {
      maybeTeam <- dataService.teams.find(event.teamId)
      didDelete <- maybeTeam.map { team =>
        dataService.scheduledMessages.deleteFor(text, team)
      }.getOrElse(Future.successful(false))
      scheduled <- maybeTeam.map { team =>
        dataService.scheduledMessages.allForTeam(team)
      }.getOrElse(Future.successful(Seq()))
      listResponses <- Future.sequence(scheduled.map(ea => ea.listResponse(ea.id, ea.team.id, dataService, configuration, includeChannel = true)))
    } yield {
      val msg = if (didDelete) {
        s"OK, I unscheduled `$text`"
      } else {
        val alternativesMessage = if(scheduled.isEmpty) {
          "You don’t currently have anything scheduled."
        } else {
          s"Here’s what you have scheduled currently:\n\n${listResponses.mkString}"
        }
        s"I couldn't find `$text` scheduled. $alternativesMessage"
      }
      SimpleTextResult(event, None, msg, forcePrivateResponse = false)
    }
  }

}

package models.behaviors.builtins

import models.behaviors.{BotResult, SimpleTextResult}
import services.slack.NewMessageEvent
import services.{AWSLambdaService, DataService}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class UnscheduleBehavior(
                               text: String,
                               event: NewMessageEvent,
                               lambdaService: AWSLambdaService,
                               dataService: DataService
                             ) extends BuiltinBehavior {

  def result: Future[BotResult] = {
    for {
      maybeTeam <- dataService.teams.find(event.teamId)
      didDelete <- maybeTeam.map { team =>
        dataService.scheduledMessages.deleteFor(text, team)
      }.getOrElse(Future.successful(false))
      scheduled <- maybeTeam.map { team =>
        dataService.scheduledMessages.allForTeam(team)
      }.getOrElse(Future.successful(Seq()))
    } yield {
      val msg = if (didDelete) {
        s"OK, I unscheduled `$text`"
      } else {
        val alternativesMessage = if(scheduled.isEmpty) {
          "You don't currently have anything scheduled."
        } else {
          s"Currently scheduled:\n\n${scheduled.map(_.listResponse).mkString("\n\n")}"
        }
        s"I couldn't find `$text` scheduled. $alternativesMessage"
      }
      SimpleTextResult(msg, forcePrivateResponse = false)
    }
  }

}

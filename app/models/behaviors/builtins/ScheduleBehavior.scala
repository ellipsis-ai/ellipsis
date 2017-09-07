package models.behaviors.builtins

import akka.actor.ActorSystem
import models.behaviors.events.Event
import models.behaviors.{BotResult, SimpleTextResult}
import services.{AWSLambdaService, DataService}

import scala.concurrent.{ExecutionContext, Future}


case class ScheduleBehavior(
                             text: String,
                             isForIndividualMembers: Boolean,
                             recurrence: String,
                             event: Event,
                             lambdaService: AWSLambdaService,
                             dataService: DataService
                             ) extends BuiltinBehavior {

  def result(implicit actorSystem: ActorSystem, ec: ExecutionContext): Future[BotResult] = {
    for {
      user <- event.ensureUser(dataService)
      maybeTeam <- dataService.teams.find(user.teamId)
      maybeScheduledMessage <- maybeTeam.map { team =>
        dataService.scheduledMessages.maybeCreateWithRecurrenceText(text, recurrence, user, team, event.maybeChannel, isForIndividualMembers)
      }.getOrElse(Future.successful(None))
      responseText <- maybeScheduledMessage.map { scheduledMessage =>
        scheduledMessage.successResponse(dataService)
      }.getOrElse(Future.successful(s"Sorry, I don’t know how to schedule `$recurrence`"))
    } yield {
      SimpleTextResult(event, None, responseText, forcePrivateResponse = false)
    }
  }

}

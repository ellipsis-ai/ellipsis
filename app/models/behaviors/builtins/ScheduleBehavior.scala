package models.behaviors.builtins

import models.behaviors.events.MessageEvent
import models.behaviors.{BotResult, SimpleTextResult}
import services.{AWSLambdaService, DataService}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


case class ScheduleBehavior(
                             text: String,
                             isForIndividualMembers: Boolean,
                             recurrence: String,
                             event: MessageEvent,
                             lambdaService: AWSLambdaService,
                             dataService: DataService
                             ) extends BuiltinBehavior {

  def result: Future[BotResult] = {
    for {
      user <- event.ensureUser(dataService)
      maybeTeam <- dataService.teams.find(user.teamId)
      maybeScheduledMessage <- maybeTeam.map { team =>
        dataService.scheduledMessages.maybeCreateFor(text, recurrence, user, team, event.maybeChannel, isForIndividualMembers)
      }.getOrElse(Future.successful(None))
    } yield {
      val responseText = maybeScheduledMessage.map { scheduledMessage =>
        scheduledMessage.successResponse
      }.getOrElse(s"Sorry, I don't know how to schedule `$recurrence`")

      SimpleTextResult(event, responseText, forcePrivateResponse = false)
    }
  }

}

package models.behaviors.builtins

import models.behaviors.{BotResult, SimpleTextResult}
import services.slack.{NewMessageEvent, NewSlackMessageEvent}
import services.{AWSLambdaService, DataService}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


case class ScheduleBehavior(
                             text: String,
                             isForIndividualMembers: Boolean,
                             recurrence: String,
                             event: NewMessageEvent,
                             lambdaService: AWSLambdaService,
                             dataService: DataService
                             ) extends BuiltinBehavior {

  def maybeChannel: Option[String] = {
    event match {
      case e: NewSlackMessageEvent => Some(e.channel)
      case _ => None
    }
  }

  def result: Future[BotResult] = {
    for {
      user <- event.ensureUser(dataService)
      maybeTeam <- dataService.teams.find(user.teamId)
      maybeScheduledMessage <- maybeTeam.map { team =>
        dataService.scheduledMessages.maybeCreateFor(text, recurrence, user, team, maybeChannel, isForIndividualMembers)
      }.getOrElse(Future.successful(None))
    } yield {
      val responseText = maybeScheduledMessage.map { scheduledMessage =>
        scheduledMessage.successResponse
      }.getOrElse(s"Sorry, I don't know how to schedule `$recurrence`")

      SimpleTextResult(responseText, forcePrivateResponse = false)
    }
  }

}

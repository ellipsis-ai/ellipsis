package models.bots.builtins

import models.bots.events.{MessageContext, SlackMessageContext}
import models.bots.{BehaviorResult, SimpleTextResult}
import services.{AWSLambdaService, DataService}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


case class ScheduleBehavior(
                             text: String,
                             recurrence: String,
                             messageContext: MessageContext,
                             lambdaService: AWSLambdaService,
                             dataService: DataService
                             ) extends BuiltinBehavior {

  def maybeChannel: Option[String] = {
    messageContext match {
      case mc: SlackMessageContext => Some(mc.message.channel)
      case _ => None
    }
  }

  def result: Future[BehaviorResult] = {
    for {
      maybeTeam <- dataService.teams.find(messageContext.teamId)
      maybeScheduledMessage <- maybeTeam.map { team =>
        dataService.scheduledMessages.maybeCreateFor(text, recurrence, team, maybeChannel)
      }.getOrElse(Future.successful(None))
    } yield {
      val responseText = maybeScheduledMessage.map { scheduledMessage =>
        scheduledMessage.successResponse
      }.getOrElse(s"Sorry, I don't know how to schedule `$recurrence`")

      SimpleTextResult(responseText)
    }
  }

}

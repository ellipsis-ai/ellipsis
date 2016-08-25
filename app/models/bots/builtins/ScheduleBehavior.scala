package models.bots.builtins

import models.Team
import models.bots.events.{MessageContext, SlackMessageContext}
import models.bots.{BehaviorResult, ScheduledMessageQueries, SimpleTextResult}
import services.AWSLambdaService
import slick.driver.PostgresDriver.api._

import scala.concurrent.ExecutionContext.Implicits.global


case class ScheduleBehavior(
                             text: String,
                             recurrence: String,
                             messageContext: MessageContext,
                             lambdaService: AWSLambdaService
                             ) extends BuiltinBehavior {

  def maybeChannel: Option[String] = {
    messageContext match {
      case mc: SlackMessageContext => Some(mc.message.channel)
      case _ => None
    }
  }

  def result: DBIO[BehaviorResult] = {
    for {
      maybeTeam <- Team.find(messageContext.teamId)
      maybeScheduledMessage <- maybeTeam.map { team =>
        ScheduledMessageQueries.maybeCreateFor(text, recurrence, team, maybeChannel)
      }.getOrElse(DBIO.successful(None))
    } yield {
      val responseText = maybeScheduledMessage.map { scheduledMessage =>
        scheduledMessage.successResponse
      }.getOrElse(s"Sorry, I don't know how to schedule `$recurrence`")

      SimpleTextResult(responseText)
    }
  }

}

package models.behaviors.builtins

import akka.actor.ActorSystem
import models.behaviors.events.Event
import models.behaviors.scheduling.Scheduled
import models.behaviors.scheduling.scheduledmessage.ScheduledMessage
import models.behaviors.{BotResult, SimpleTextResult}
import services.{AWSLambdaService, DataService}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


case class ListScheduledBehavior(
                                  event: Event,
                                  maybeChannel: Option[String],
                                  lambdaService: AWSLambdaService,
                                  dataService: DataService
                                 ) extends BuiltinBehavior {

  private def example: String = {
    s"""
       |```
       |${event.botPrefix}schedule "go bananas" every day at 3pm
       |```
       """.stripMargin
  }

  private def noMessagesResponse: String = {
    if (maybeChannel.isDefined) {
      s"""You haven’t yet scheduled anything in this channel. To schedule, try something like:
         |
         |$example
         |
         |$otherCommand
       """.stripMargin
    } else {
      s"""Nothing has been scheduled for this team. To schedule something, try:
         |
         |$example
       """.stripMargin
    }
  }

  private def otherCommand: String = {
    if (maybeChannel.isDefined) {
      s"""To see what is scheduled across all channels:
         |
         |```
         |${event.botPrefix}all scheduled
         |```
       """.stripMargin
    } else {
      s"""To see what is scheduled in just this channel:
         |
         |```
         |${event.botPrefix}scheduled
         |```
       """.stripMargin
    }
  }

  private def intro: String = {
    maybeChannel.map { channel =>
      if (event.isPublicChannel) {
        s"Here’s what you have scheduled in <#$channel>:"
      } else {
        "Here’s what you have scheduled in this channel:"
      }
    }.getOrElse {
      "Here’s everything that has been scheduled for this team:"
    }
  }

  private def unscheduleCommand: String = {
    s"""
      |You can unschedule by typing something like:
      |
      |```
      |${event.botPrefix}unschedule "go bananas"
      |```
    """.stripMargin
  }

  def responseFor(scheduled: Seq[Scheduled]): Future[String] = {
    Future.sequence(scheduled.map(_.listResponse(dataService, maybeChannel.isEmpty))).map { listResponses =>
      s"""$intro
        |
        |${listResponses.mkString}
        |
        |You can unschedule by typing something like:
        |
        |```
        |${event.botPrefix}unschedule "go bananas"
        |```
        |
        |$otherCommand
      """.stripMargin
    }
  }

  def result(implicit actorSystem: ActorSystem): Future[BotResult] = {
    for {
      maybeTeam <- dataService.teams.find(event.teamId)
      scheduled <- maybeTeam.map { team =>
        maybeChannel.map { channel =>
          Scheduled.allForChannel(team, channel, dataService)
        }.getOrElse {
          Scheduled.allForTeam(team, dataService)
        }
      }.getOrElse(Future.successful(Seq()))
      responseText <- if (scheduled.isEmpty) {
        Future.successful(noMessagesResponse)
      } else {
        responseFor(scheduled)
      }
    } yield {
      SimpleTextResult(event, None, responseText, forcePrivateResponse = false)
    }
  }

}

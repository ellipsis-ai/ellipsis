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
                                  lambdaService: AWSLambdaService,
                                  dataService: DataService
                                 ) extends BuiltinBehavior {

  lazy val noMessagesResponse: String =
    s"""You haven’t yet scheduled anything. To do so, try something like:
      |
      |```
      |${event.botPrefix}schedule "go bananas" every day at 3pm
      |```
    """.stripMargin

  private def channelText: String = {
    event.maybeChannel.map { channel =>
      if (event.isPublicChannel) {
        s" in <#$channel>"
      } else {
        " in this channel"
      }
    }.getOrElse("")
  }

  def responseFor(scheduled: Seq[Scheduled]): Future[String] = {
    Future.sequence(scheduled.map(_.listResponse(dataService))).map { listResponses =>
      s"""Here’s what you have scheduled$channelText:
        |
        |${listResponses.mkString}
        |
        |You can unschedule by typing something like:
        |
        |```
        |${event.botPrefix}unschedule "go bananas"
        |```
      """.stripMargin
    }
  }

  def result(implicit actorSystem: ActorSystem): Future[BotResult] = {
    for {
      maybeTeam <- dataService.teams.find(event.teamId)
      scheduled <- maybeTeam.map { team =>
        event.maybeChannel.map { channel =>
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

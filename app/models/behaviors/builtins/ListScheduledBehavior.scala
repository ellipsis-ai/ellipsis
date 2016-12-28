package models.behaviors.builtins

import models.behaviors.scheduledmessage.ScheduledMessage
import models.behaviors.{BotResult, SimpleTextResult}
import services.slack.MessageEvent
import services.{AWSLambdaService, DataService}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


case class ListScheduledBehavior(
                                  event: MessageEvent,
                                  lambdaService: AWSLambdaService,
                                  dataService: DataService
                                 ) extends BuiltinBehavior {

  lazy val noMessagesResponse: String =
    """You haven't yet scheduled anything. To do so, try something like:
      |
      |@ellipsis: schedule `some ellipsis skill` every day at 3pm
    """.stripMargin

  def responseForMessages(messages: Seq[ScheduledMessage]): String = {
    s"""Here is what you have scheduled:
       |
       |${messages.map(_.listResponse).mkString("\n\n")}
       |
       |You can unschedule by typing something like:
       |
       |@ellipsis: unschedule `some ellipsis skill`
     """.stripMargin
  }

  def result: Future[BotResult] = {
    for {
      maybeTeam <- dataService.teams.find(event.teamId)
      scheduled <- maybeTeam.map { team =>
        dataService.scheduledMessages.allForTeam(team)
      }.getOrElse(Future.successful(Seq()))
    } yield {
      val responseText = if (scheduled.isEmpty) {
        noMessagesResponse
      } else {
        responseForMessages(scheduled)
      }

      SimpleTextResult(responseText, forcePrivateResponse = false)
    }
  }

}

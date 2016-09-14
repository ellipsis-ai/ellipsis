package models.bots.builtins

import models.bots.events.MessageContext
import models.bots.scheduledmessage.ScheduledMessage
import models.bots.{BehaviorResult, SimpleTextResult}
import services.{AWSLambdaService, DataService}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


case class ListScheduledBehavior(
                                 messageContext: MessageContext,
                                 lambdaService: AWSLambdaService,
                                 dataService: DataService
                                 ) extends BuiltinBehavior {

  lazy val noMessagesResponse: String =
    """You haven't yet scheduled anything. To do so, try something like:
      |
      |@ellipsis: schedule `some ellipsis behavior` every day at 3pm
    """.stripMargin

  def responseForMessages(messages: Seq[ScheduledMessage]): String = {
    s"""Here is what you have scheduled:
       |
       |${messages.map(_.listResponse).mkString("\n\n")}
       |
       |You can unschedule by typing something like:
       |
       |@ellipsis: unschedule `some ellipsis behavior`
     """.stripMargin
  }

  def result: Future[BehaviorResult] = {
    for {
      maybeTeam <- dataService.teams.find(messageContext.teamId)
      scheduled <- maybeTeam.map { team =>
        dataService.scheduledMessages.allForTeam(team)
      }.getOrElse(Future.successful(Seq()))
    } yield {
      val responseText = if (scheduled.isEmpty) {
        noMessagesResponse
      } else {
        responseForMessages(scheduled)
      }

      SimpleTextResult(responseText)
    }
  }

}

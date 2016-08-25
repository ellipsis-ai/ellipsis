package models.bots.builtins

import models.Team
import models.bots.events.MessageContext
import models.bots.{ScheduledMessage, ScheduledMessageQueries}
import services.AWSLambdaService
import slick.driver.PostgresDriver.api._

import scala.concurrent.ExecutionContext.Implicits.global


case class ListScheduledBehavior(
                                 messageContext: MessageContext,
                                 lambdaService: AWSLambdaService
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
     """.stripMargin
  }

  def run: DBIO[Unit] = {
    for {
      maybeTeam <- Team.find(messageContext.teamId)
      scheduled <- maybeTeam.map { team =>
        ScheduledMessageQueries.allForTeam(team)
      }.getOrElse(DBIO.successful(Seq()))
    } yield {
      val responseText = if (scheduled.isEmpty) {
        noMessagesResponse
      } else {
        responseForMessages(scheduled)
      }

      messageContext.sendMessage(responseText)
    }
  }

}

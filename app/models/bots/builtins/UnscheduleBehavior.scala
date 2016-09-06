package models.bots.builtins

import models.bots.events.MessageContext
import models.bots.{BehaviorResult, ScheduledMessageQueries, SimpleTextResult}
import services.{AWSLambdaService, DataService}
import slick.driver.PostgresDriver.api._

import scala.concurrent.ExecutionContext.Implicits.global

case class UnscheduleBehavior(
                             text: String,
                             messageContext: MessageContext,
                             lambdaService: AWSLambdaService,
                             dataService: DataService
                             ) extends BuiltinBehavior {

  def result: DBIO[BehaviorResult] = {
    for {
      maybeTeam <- DBIO.from(dataService.teams.find(messageContext.teamId))
      didDelete <- maybeTeam.map { team =>
        ScheduledMessageQueries.deleteFor(text, team)
      }.getOrElse(DBIO.successful(false))
      scheduled <- maybeTeam.map { team =>
        ScheduledMessageQueries.allForTeam(team)
      }.getOrElse(DBIO.successful(Seq()))
    } yield {
      val msg = if (didDelete) {
        s"OK, I unscheduled `$text`"
      } else {
        val alternativesMessage = if(scheduled.isEmpty) {
          "You don't currently have anything scheduled."
        } else {
          s"Currently scheduled:\n\n${scheduled.map(_.listResponse).mkString("\n\n")}"
        }
        s"I couldn't find `$text` scheduled. $alternativesMessage"
      }
      SimpleTextResult(msg)
    }
  }

}

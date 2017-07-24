package models.behaviors.scheduling.scheduledmessage

import java.time.OffsetDateTime

import models.accounts.user.User
import models.behaviors.scheduling.recurrence.Recurrence
import models.team.Team
import slick.dbio.DBIO

import scala.concurrent.Future

trait ScheduledMessageService {

  def maybeNextToBeSentAction(when: OffsetDateTime): DBIO[Option[ScheduledMessage]]

  def allForTeam(team: Team): Future[Seq[ScheduledMessage]]

  def allForChannel(team: Team, channel: String): Future[Seq[ScheduledMessage]]

  def find(id: String): Future[Option[ScheduledMessage]]

  def findForTeam(id: String, team: Team): Future[Option[ScheduledMessage]]

  def save(message: ScheduledMessage): Future[ScheduledMessage]

  def updateNextTriggeredForAction(message: ScheduledMessage): DBIO[ScheduledMessage]

  def maybeCreateWithRecurrenceText(
                      text: String,
                      recurrenceText: String,
                      user: User,
                      team: Team,
                      maybeChannel: Option[String],
                      isForIndividualMembers: Boolean
                    ): Future[Option[ScheduledMessage]]

  def createFor(
                 text: String,
                 recurrence: Recurrence,
                 user: User,
                 team: Team,
                 maybeChannel: Option[String],
                 isForIndividualMembers: Boolean
               ): Future[ScheduledMessage]

  def deleteFor(text: String, team: Team): Future[Boolean]

  def delete(scheduledMessage: ScheduledMessage): Future[Boolean]

}

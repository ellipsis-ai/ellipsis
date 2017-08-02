package models.behaviors.scheduling.scheduledbehavior

import java.time.OffsetDateTime

import models.accounts.user.User
import models.behaviors.behavior.Behavior
import models.behaviors.scheduling.recurrence.Recurrence
import models.team.Team
import slick.dbio.DBIO

import scala.concurrent.Future

trait ScheduledBehaviorService {

  def allActiveForTeam(team: Team): Future[Seq[ScheduledBehavior]]

  def allActiveForChannel(team: Team, channel: String): Future[Seq[ScheduledBehavior]]

  def find(id: String): Future[Option[ScheduledBehavior]]

  def findForTeam(id: String, team: Team): Future[Option[ScheduledBehavior]]

  def allForBehavior(behavior: Behavior, maybeUser: Option[User], maybeChannel: Option[String]): Future[Seq[ScheduledBehavior]]

  def maybeNextToBeSentAction(when: OffsetDateTime): DBIO[Option[ScheduledBehavior]]

  def save(scheduledBehavior: ScheduledBehavior): Future[ScheduledBehavior]

  def updateNextTriggeredForAction(scheduledBehavior: ScheduledBehavior): DBIO[ScheduledBehavior]

  def maybeCreateWithRecurrenceText(
                      behavior: Behavior,
                      arguments: Map[String, String],
                      recurrenceText: String,
                      user: User,
                      team: Team,
                      maybeChannel: Option[String],
                      isForIndividualMembers: Boolean
                    ): Future[Option[ScheduledBehavior]]

  def createFor(
                 behavior: Behavior,
                 arguments: Map[String, String],
                 recurrence: Recurrence,
                 user: User,
                 team: Team,
                 maybeChannel: Option[String],
                 isForIndividualMembers: Boolean
               ): Future[ScheduledBehavior]

  def delete(scheduledBehavior: ScheduledBehavior): Future[Boolean]

}

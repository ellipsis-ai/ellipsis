package models.behaviors.scheduling.scheduledbehavior

import models.accounts.user.User
import models.behaviors.behavior.Behavior
import models.behaviors.scheduling.recurrence.Recurrence
import models.team.Team

import scala.concurrent.Future

trait ScheduledBehaviorService {

  def allToBeSent: Future[Seq[ScheduledBehavior]]

  def allForTeam(team: Team): Future[Seq[ScheduledBehavior]]

  def allForChannel(team: Team, channel: String): Future[Seq[ScheduledBehavior]]

  def find(id: String): Future[Option[ScheduledBehavior]]

  def findForTeam(id: String, team: Team): Future[Option[ScheduledBehavior]]

  def allForBehavior(behavior: Behavior, maybeUser: Option[User], maybeChannel: Option[String]): Future[Seq[ScheduledBehavior]]

  def save(scheduledBehavior: ScheduledBehavior): Future[ScheduledBehavior]

  def updateNextTriggeredFor(scheduledBehavior: ScheduledBehavior): Future[ScheduledBehavior]

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

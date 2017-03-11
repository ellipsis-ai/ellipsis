package models.behaviors.scheduling.scheduledbehavior

import models.accounts.user.User
import models.behaviors.behavior.Behavior
import models.team.Team

import scala.concurrent.Future

trait ScheduledBehaviorService {

  def allToBeSent: Future[Seq[ScheduledBehavior]]

  def allForTeam(team: Team): Future[Seq[ScheduledBehavior]]

  def find(id: String): Future[Option[ScheduledBehavior]]

  def allForBehavior(behavior: Behavior): Future[Seq[ScheduledBehavior]]

  def save(scheduledBehavior: ScheduledBehavior): Future[ScheduledBehavior]

  def updateNextTriggeredFor(scheduledBehavior: ScheduledBehavior): Future[ScheduledBehavior]

  def maybeCreateFor(
                      behavior: Behavior,
                      arguments: Map[String, String],
                      recurrenceText: String,
                      user: User,
                      team: Team,
                      maybeChannel: Option[String],
                      isForIndividualMembers: Boolean
                    ): Future[Option[ScheduledBehavior]]

  def deleteFor(behavior: Behavior, team: Team): Future[Boolean]

}

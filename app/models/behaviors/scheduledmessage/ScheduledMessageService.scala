package models.behaviors.scheduledmessage

import models.accounts.user.User
import models.team.Team

import scala.concurrent.Future

trait ScheduledMessageService {

  def allToBeSent: Future[Seq[ScheduledMessage]]

  def allForTeam(team: Team): Future[Seq[ScheduledMessage]]

  def save(trigger: ScheduledMessage): Future[ScheduledMessage]

  def maybeCreateFor(
                      text: String,
                      recurrenceText: String,
                      user: User,
                      team: Team,
                      maybeChannelName: Option[String]
                    ): Future[Option[ScheduledMessage]]

  def deleteFor(text: String, team: Team): Future[Boolean]

}

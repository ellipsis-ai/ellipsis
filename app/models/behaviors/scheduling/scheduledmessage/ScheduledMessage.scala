package models.behaviors.scheduling.scheduledmessage

import java.time.OffsetDateTime

import models.accounts.slack.botprofile.SlackBotProfile
import models.accounts.user.User
import models.behaviors.events.{ScheduledEvent, SlackMessageEvent}
import models.behaviors.scheduling.Scheduled
import models.behaviors.scheduling.recurrence.Recurrence
import models.team.Team
import services.DataService

import scala.concurrent.Future

case class ScheduledMessage(
                             id: String,
                             text: String,
                             maybeUser: Option[User],
                             team: Team,
                             maybeChannelName: Option[String],
                             isForIndividualMembers: Boolean,
                             recurrence: Recurrence,
                             nextSentAt: OffsetDateTime,
                             createdAt: OffsetDateTime
                           ) extends Scheduled {

  val displayText = text

  def eventFor(channelName: String, slackUserId: String, profile: SlackBotProfile): ScheduledEvent = {
    ScheduledEvent(SlackMessageEvent(profile, channelName, None, slackUserId, text, "ts"), this)
  }

  def withUpdatedNextTriggeredFor(when: OffsetDateTime): ScheduledMessage = {
    this.copy(nextSentAt = recurrence.nextAfter(when))
  }

  def updateNextTriggeredFor(dataService: DataService): Future[ScheduledMessage] = {
    dataService.scheduledMessages.updateNextTriggeredFor(this)
  }

  def toRaw: RawScheduledMessage = {
    RawScheduledMessage(
      id,
      text,
      maybeUser.map(_.id),
      team.id,
      maybeChannelName,
      isForIndividualMembers,
      recurrence.id,
      nextSentAt,
      createdAt
    )
  }
}

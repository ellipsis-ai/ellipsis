package models.behaviors.scheduling.scheduledbehavior

import java.time.OffsetDateTime

import models.accounts.slack.botprofile.SlackBotProfile
import models.accounts.user.User
import models.behaviors.behavior.Behavior
import models.behaviors.events.{RunEvent, ScheduledEvent}
import models.behaviors.scheduling.Scheduled
import models.behaviors.scheduling.recurrence.Recurrence
import models.team.Team

case class ScheduledBehavior(
                             id: String,
                             behavior: Behavior,
                             maybeUser: Option[User],
                             team: Team,
                             maybeChannelName: Option[String],
                             isForIndividualMembers: Boolean,
                             recurrence: Recurrence,
                             nextSentAt: OffsetDateTime,
                             createdAt: OffsetDateTime
                           ) extends Scheduled {

  val displayText: String = s"Running ${behavior.id}"

  def eventFor(channelName: String, slackUserId: String, profile: SlackBotProfile): ScheduledEvent = {
    ScheduledEvent(RunEvent(profile, behavior, Map(), channelName, None, slackUserId, "ts"), this)
  }

  def withUpdatedNextTriggeredFor(when: OffsetDateTime): ScheduledBehavior = {
    this.copy(nextSentAt = recurrence.nextAfter(when))
  }

  def toRaw: RawScheduledBehavior = {
    RawScheduledBehavior(
      id,
      behavior.id,
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

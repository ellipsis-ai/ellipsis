package models.behaviors.scheduling.scheduledmessage

import java.time.OffsetDateTime

import models.accounts.slack.botprofile.SlackBotProfile
import models.accounts.user.User
import models.behaviors.events.{ScheduledEvent, SlackMessageEvent}
import models.behaviors.scheduling.Scheduled
import models.behaviors.scheduling.recurrence.Recurrence
import models.team.Team
import services.DataService
import slack.api.SlackApiClient
import slick.dbio.DBIO
import utils.SlackTimestamp

import scala.concurrent.Future

case class ScheduledMessage(
                             id: String,
                             text: String,
                             maybeUser: Option[User],
                             team: Team,
                             maybeChannel: Option[String],
                             isForIndividualMembers: Boolean,
                             recurrence: Recurrence,
                             nextSentAt: OffsetDateTime,
                             createdAt: OffsetDateTime
                           ) extends Scheduled {

  def displayText(dataService: DataService): Future[String] = {
    Future.successful(s"`$text`")
  }

  def eventFor(channel: String, slackUserId: String, profile: SlackBotProfile, client: SlackApiClient): ScheduledEvent = {
    // TODO: Create a new class of synthetic events that doesn't need a SlackUserInfo list
    // https://github.com/ellipsis-ai/ellipsis/issues/1719
    // Scheduled messages shouldn't ever be created with Slack-formatted text, since the built-in schedule behavior already receives unformatted text
    ScheduledEvent(SlackMessageEvent(profile, channel, None, slackUserId, text, SlackTimestamp.now, client, Seq()), this)
  }

  def withUpdatedNextTriggeredFor(when: OffsetDateTime): ScheduledMessage = {
    this.copy(nextSentAt = recurrence.nextAfter(when))
  }

  def updateNextTriggeredForAction(dataService: DataService): DBIO[ScheduledMessage] = {
    dataService.scheduledMessages.updateNextTriggeredForAction(this)
  }

  def toRaw: RawScheduledMessage = {
    RawScheduledMessage(
      id,
      text,
      maybeUser.map(_.id),
      team.id,
      maybeChannel,
      isForIndividualMembers,
      recurrence.id,
      nextSentAt,
      createdAt
    )
  }
}

object ScheduledMessage {
  val tableName: String = "scheduled_messages"
}
